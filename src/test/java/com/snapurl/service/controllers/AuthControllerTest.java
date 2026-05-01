package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ForgotPasswordRequest;
import com.snapurl.service.dtos.ForgotPasswordResponse;
import com.snapurl.service.dtos.LoginRequest;
import com.snapurl.service.dtos.RefreshTokenRequest;
import com.snapurl.service.dtos.RegisterRequest;
import com.snapurl.service.dtos.ResetPasswordRequest;
import com.snapurl.service.models.Users;
import com.snapurl.service.security.JwtAuthenticationResponse;
import com.snapurl.service.service.ClientIpResolver;
import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitGuard;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private RateLimitGuard rateLimitGuard;
    @Mock
    private ClientIpResolver clientIpResolver;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService, rateLimitGuard, clientIpResolver);
        ReflectionTestUtils.setField(controller, "loginPerWindow", 5L);
        ReflectionTestUtils.setField(controller, "registerPerHour", 5L);
        ReflectionTestUtils.setField(controller, "refreshPerWindow", 10L);
        ReflectionTestUtils.setField(controller, "forgotPasswordPerHour", 4L);
        ReflectionTestUtils.setField(controller, "resetPasswordPerHour", 6L);
        ReflectionTestUtils.setField(controller, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(controller, "refreshCookieName", "SNAPURL_REFRESH_TOKEN");
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", false);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");
    }

    @Test
    void loginReturns429WhenRateLimitIsExceeded() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("password123");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(
                eq("snapurl:rate-limit:login:127.0.0.1:user@gmail.com"),
                eq(5L),
                any(),
                eq("auth_login"),
                eq("Too many login attempts. Please try again later.")
        )).thenThrow(new RateLimitExceededException(
                "Too many login attempts. Please try again later.",
                new RateLimitResult(false, 5, 6, 0, 900)
        ));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> controller.loginUser(loginRequest, httpServletRequest, httpServletResponse)
        );

        assertEquals("Too many login attempts. Please try again later.", exception.getMessage());
        assertEquals(900, exception.getRateLimitResult().getRetryAfterSeconds());
    }

    @Test
    void registerDelegatesToUserServiceWithNormalizedRole() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("tester");
        registerRequest.setEmail("user@gmail.com");
        registerRequest.setPassword("password123");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(eq("snapurl:rate-limit:register:127.0.0.1"), eq(5L), any(), eq("auth_register"), any()))
                .thenReturn(new RateLimitResult(true, 5, 1, 4, 0));
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();

        var response = controller.register(registerRequest, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User Registered Successfully", response.getBody());
        assertEquals("5", response.getHeaders().getFirst("X-RateLimit-Limit"));
        verify(userService).registerUser(argThat((Users user) ->
                "tester".equals(user.getUsername())
                        && "user@gmail.com".equals(user.getEmail())
                        && "password123".equals(user.getPassword())
                        && "ROLE_USER".equals(user.getRole())
        ));
    }

    @Test
    void loginReturnsTokensAndRateLimitHeadersWhenAllowed() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("password123");

        JwtAuthenticationResponse authResponse = new JwtAuthenticationResponse("access", "refresh", "Bearer");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(eq("snapurl:rate-limit:login:127.0.0.1:user@gmail.com"), eq(5L), any(), eq("auth_login"), any()))
                .thenReturn(new RateLimitResult(true, 5, 2, 3, 0));
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();
        when(userService.loginUser(loginRequest)).thenReturn(authResponse);

        var response = controller.loginUser(loginRequest, httpServletRequest, httpServletResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("5", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("3", response.getHeaders().getFirst("X-RateLimit-Remaining"));
        JwtAuthenticationResponse body = (JwtAuthenticationResponse) response.getBody();
        assertEquals("access", body.getAccessToken());
        assertEquals(null, body.getRefreshToken());
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("SNAPURL_REFRESH_TOKEN=refresh"));
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("HttpOnly"));
    }

    @Test
    void refreshTokenDelegatesToUserService() {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token");

        JwtAuthenticationResponse authResponse = new JwtAuthenticationResponse("new-access", "new-refresh", "Bearer");
        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(eq("snapurl:rate-limit:refresh:127.0.0.1"), eq(10L), any(), eq("auth_refresh"), any()))
                .thenReturn(new RateLimitResult(true, 10, 1, 9, 0));
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();
        when(userService.refreshAccessToken(refreshTokenRequest)).thenReturn(authResponse);

        var response = controller.refreshToken(refreshTokenRequest, httpServletRequest, httpServletResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JwtAuthenticationResponse body = (JwtAuthenticationResponse) response.getBody();
        assertEquals("new-access", body.getAccessToken());
        assertEquals(null, body.getRefreshToken());
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("SNAPURL_REFRESH_TOKEN=new-refresh"));
    }

    @Test
    void forgotPasswordReturnsGenericResponse() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@gmail.com");
        ForgotPasswordResponse serviceResponse = new ForgotPasswordResponse(
                "If an account exists for that email, a reset code has been prepared.",
                "123456"
        );

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(eq("snapurl:rate-limit:forgot-password:127.0.0.1"), eq(4L), any(), eq("auth_forgot_password"), any()))
                .thenReturn(new RateLimitResult(true, 4, 1, 3, 0));
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();
        when(userService.requestPasswordReset("user@gmail.com")).thenReturn(serviceResponse);

        var response = controller.forgotPassword(request, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    @Test
    void resetPasswordDelegatesToUserService() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("123456");
        request.setPassword("newPassword123");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(eq("snapurl:rate-limit:reset-password:127.0.0.1"), eq(6L), any(), eq("auth_reset_password"), any()))
                .thenReturn(new RateLimitResult(true, 6, 1, 5, 0));
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();

        var response = controller.resetPassword(request, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset successfully", response.getBody());
        verify(userService).resetPassword(request);
    }

    @Test
    void registerReturns429WhenRateLimitIsExceeded() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("tester");
        registerRequest.setEmail("user@gmail.com");
        registerRequest.setPassword("password123");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(
                eq("snapurl:rate-limit:register:127.0.0.1"),
                eq(5L),
                any(),
                eq("auth_register"),
                eq("Too many registration attempts. Please try again later.")
        )).thenThrow(new RateLimitExceededException(
                "Too many registration attempts. Please try again later.",
                new RateLimitResult(false, 5, 6, 0, 3600)
        ));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> controller.register(registerRequest, httpServletRequest)
        );

        assertEquals("Too many registration attempts. Please try again later.", exception.getMessage());
    }

    @Test
    void forgotPasswordReturns429WhenRateLimitIsExceeded() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@gmail.com");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(
                eq("snapurl:rate-limit:forgot-password:127.0.0.1"),
                eq(4L),
                any(),
                eq("auth_forgot_password"),
                eq("Too many password reset requests. Please try again later.")
        )).thenThrow(new RateLimitExceededException(
                "Too many password reset requests. Please try again later.",
                new RateLimitResult(false, 4, 5, 0, 3600)
        ));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> controller.forgotPassword(request, httpServletRequest)
        );

        assertEquals("Too many password reset requests. Please try again later.", exception.getMessage());
    }
}
