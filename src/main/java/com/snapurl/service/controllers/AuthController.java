package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ForgotPasswordRequest;
import com.snapurl.service.dtos.ForgotPasswordResponse;
import com.snapurl.service.dtos.LoginRequest;
import com.snapurl.service.dtos.RefreshTokenRequest;
import com.snapurl.service.dtos.RegisterRequest;
import com.snapurl.service.dtos.ResetPasswordRequest;
import com.snapurl.service.models.Users;
import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.RateLimitService;
import com.snapurl.service.service.AppMetricsService;
import com.snapurl.service.service.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {


    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final AppMetricsService appMetricsService;
    @Value("${snapurl.rate-limit.login-per-15-minutes:5}")
    private long loginPerWindow;
    @Value("${snapurl.rate-limit.register-per-hour:5}")
    private long registerPerHour;
    @Value("${snapurl.rate-limit.refresh-per-15-minutes:30}")
    private long refreshPerWindow;
    @Value("${snapurl.rate-limit.forgot-password-per-hour:5}")
    private long forgotPasswordPerHour;
    @Value("${snapurl.rate-limit.reset-password-per-hour:10}")
    private long resetPasswordPerHour;
    @Value("${snapurl.rate-limit.trust-forwarded-header:false}")
    private boolean trustForwardedHeader;

    public AuthController(UserService userService, RateLimitService rateLimitService, AppMetricsService appMetricsService) {
        this.userService = userService;
        this.rateLimitService = rateLimitService;
        this.appMetricsService = appMetricsService;
    }

    @PostMapping("/public/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        RateLimitResult rateLimitResult = rateLimitService.check(
                "snapurl:rate-limit:register:" + clientIp,
                registerPerHour,
                Duration.ofHours(1)
        );
        if (!rateLimitResult.isAllowed()) {
            appMetricsService.recordRateLimitHit("auth_register");
            log.warn("Registration rate limit exceeded for ip={}", clientIp);
            throw new RateLimitExceededException("Too many registration attempts. Please try again later.", rateLimitResult);
        }

        Users user = new Users();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setRole("ROLE_USER");
        userService.registerUser(user);
        log.info("User registration succeeded for email={} ip={}", registerRequest.getEmail(), clientIp);
        return withRateLimitHeaders(ResponseEntity.ok(), rateLimitResult).body("User Registered Successfully");
    }

    @PostMapping("/public/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        RateLimitResult rateLimitResult = rateLimitService.check(
                "snapurl:rate-limit:login:" + clientIp + ":" + normalizeEmail(loginRequest.getEmail()),
                loginPerWindow,
                Duration.ofMinutes(15)
        );
        if (!rateLimitResult.isAllowed()) {
            appMetricsService.recordRateLimitHit("auth_login");
            log.warn("Login rate limit exceeded for email={} ip={}", normalizeEmail(loginRequest.getEmail()), clientIp);
            throw new RateLimitExceededException("Too many login attempts. Please try again later.", rateLimitResult);
        }
        log.info("Login request accepted for email={} ip={}", normalizeEmail(loginRequest.getEmail()), clientIp);
        return withRateLimitHeaders(ResponseEntity.ok(), rateLimitResult).body(userService.loginUser(loginRequest));
    }

    @PostMapping("/public/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        RateLimitResult rateLimitResult = rateLimitService.check(
                "snapurl:rate-limit:refresh:" + clientIp,
                refreshPerWindow,
                Duration.ofMinutes(15)
        );
        if (!rateLimitResult.isAllowed()) {
            appMetricsService.recordRateLimitHit("auth_refresh");
            log.warn("Refresh token rate limit exceeded for ip={}", clientIp);
            throw new RateLimitExceededException("Too many token refresh attempts. Please try again later.", rateLimitResult);
        }

        log.info("Refresh token request accepted for ip={}", clientIp);
        return withRateLimitHeaders(ResponseEntity.ok(), rateLimitResult).body(userService.refreshAccessToken(refreshTokenRequest));
    }

    @PostMapping("/public/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        RateLimitResult rateLimitResult = rateLimitService.check(
                "snapurl:rate-limit:forgot-password:" + clientIp,
                forgotPasswordPerHour,
                Duration.ofHours(1)
        );
        if (!rateLimitResult.isAllowed()) {
            appMetricsService.recordRateLimitHit("auth_forgot_password");
            log.warn("Forgot-password rate limit exceeded for email={} ip={}", normalizeEmail(forgotPasswordRequest.getEmail()), clientIp);
            throw new RateLimitExceededException("Too many password reset requests. Please try again later.", rateLimitResult);
        }

        log.info("Forgot-password request accepted for email={} ip={}", normalizeEmail(forgotPasswordRequest.getEmail()), clientIp);
        return withRateLimitHeaders(ResponseEntity.ok(), rateLimitResult).body(userService.requestPasswordReset(forgotPasswordRequest.getEmail()));
    }

    @PostMapping("/public/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        RateLimitResult rateLimitResult = rateLimitService.check(
                "snapurl:rate-limit:reset-password:" + clientIp,
                resetPasswordPerHour,
                Duration.ofHours(1)
        );
        if (!rateLimitResult.isAllowed()) {
            appMetricsService.recordRateLimitHit("auth_reset_password");
            log.warn("Reset-password rate limit exceeded for ip={}", clientIp);
            throw new RateLimitExceededException("Too many password reset attempts. Please try again later.", rateLimitResult);
        }

        userService.resetPassword(resetPasswordRequest);
        log.info("Password reset completed for ip={}", clientIp);
        return withRateLimitHeaders(ResponseEntity.ok(), rateLimitResult).body("Password reset successfully");
    }

    private ResponseEntity.BodyBuilder withRateLimitHeaders(ResponseEntity.BodyBuilder builder, RateLimitResult result) {
        builder.header("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        builder.header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        if (!result.isAllowed() && result.getRetryAfterSeconds() > 0) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(result.getRetryAfterSeconds()));
        }
        return builder;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (!trustForwardedHeader) {
            return request.getRemoteAddr();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        return email == null ? "anonymous" : email.trim().toLowerCase();
    }
}
