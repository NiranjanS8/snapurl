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
import com.snapurl.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
public class AuthController {


    private final UserService userService;
    private final RateLimitService rateLimitService;
    @Value("${snapurl.rate-limit.login-per-15-minutes:5}")
    private long loginPerWindow;
    @Value("${snapurl.rate-limit.trust-forwarded-header:false}")
    private boolean trustForwardedHeader;

    public AuthController(UserService userService, RateLimitService rateLimitService) {
        this.userService = userService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/public/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        Users user = new Users();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setRole("ROLE_USER");
        userService.registerUser(user);
        return ResponseEntity.ok("User Registered Successfully");
    }

    @PostMapping("/public/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        RateLimitResult rateLimitResult = rateLimitService.check(
                "snapurl:rate-limit:login:" + extractClientIp(request) + ":" + normalizeEmail(loginRequest.getEmail()),
                loginPerWindow,
                Duration.ofMinutes(15)
        );
        if (!rateLimitResult.isAllowed()) {
            throw new RateLimitExceededException("Too many login attempts. Please try again later.", rateLimitResult);
        }
        return withRateLimitHeaders(ResponseEntity.ok(), rateLimitResult).body(userService.loginUser(loginRequest));
    }

    @PostMapping("/public/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(userService.refreshAccessToken(refreshTokenRequest));
    }

    @PostMapping("/public/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        return ResponseEntity.ok(userService.requestPasswordReset(forgotPasswordRequest.getEmail()));
    }

    @PostMapping("/public/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        userService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok("Password reset successfully");
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
