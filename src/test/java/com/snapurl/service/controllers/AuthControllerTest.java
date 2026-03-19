package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ForgotPasswordRequest;
import com.snapurl.service.dtos.ForgotPasswordResponse;
import com.snapurl.service.dtos.LoginRequest;
import com.snapurl.service.dtos.RefreshTokenRequest;
import com.snapurl.service.dtos.RegisterRequest;
import com.snapurl.service.dtos.ResetPasswordRequest;
import com.snapurl.service.models.Users;
import com.snapurl.service.security.JwtAuthenticationResponse;
import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.RateLimitService;
import com.snapurl.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService, rateLimitService);
        ReflectionTestUtils.setField(controller, "loginPerWindow", 5L);
        ReflectionTestUtils.setField(controller, "trustForwardedHeader", false);
    }

    @Test
    void loginReturns429WhenRateLimitIsExceeded() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("password123");

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.check(eq("snapurl:rate-limit:login:127.0.0.1:user@gmail.com"), eq(5L), any()))
                .thenReturn(new RateLimitResult(false, 5, 6, 0, 900));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> controller.loginUser(loginRequest, httpServletRequest)
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

        var response = controller.register(registerRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User Registered Successfully", response.getBody());
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

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.check(eq("snapurl:rate-limit:login:127.0.0.1:user@gmail.com"), eq(5L), any()))
                .thenReturn(new RateLimitResult(true, 5, 2, 3, 0));
        when(userService.loginUser(loginRequest)).thenReturn(authResponse);

        var response = controller.loginUser(loginRequest, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("5", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("3", response.getHeaders().getFirst("X-RateLimit-Remaining"));
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void refreshTokenDelegatesToUserService() {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token");

        JwtAuthenticationResponse authResponse = new JwtAuthenticationResponse("new-access", "new-refresh", "Bearer");
        when(userService.refreshAccessToken(refreshTokenRequest)).thenReturn(authResponse);

        var response = controller.refreshToken(refreshTokenRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void forgotPasswordReturnsGenericResponse() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@gmail.com");
        ForgotPasswordResponse serviceResponse = new ForgotPasswordResponse(
                "If an account exists for that email, a reset code has been prepared.",
                "123456"
        );

        when(userService.requestPasswordReset("user@gmail.com")).thenReturn(serviceResponse);

        var response = controller.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    @Test
    void resetPasswordDelegatesToUserService() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("123456");
        request.setPassword("newPassword123");

        var response = controller.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset successfully", response.getBody());
        verify(userService).resetPassword(request);
    }
}
