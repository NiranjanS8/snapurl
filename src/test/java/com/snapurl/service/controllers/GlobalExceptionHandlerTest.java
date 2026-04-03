package com.snapurl.service.controllers;

import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.AccountLockedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private com.snapurl.service.service.AppMetricsService appMetricsService;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(appMetricsService);
        when(request.getRequestURI()).thenReturn("/api/urls/public/shorten");
    }

    @Test
    void handleBadRequestBuildsConsistentErrorResponse() {
        var response = handler.handleBadRequest(new IllegalArgumentException("Invalid alias"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid alias", response.getBody().getMessage());
        assertEquals("/api/urls/public/shorten", response.getBody().getPath());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    void handleRateLimitAddsRetryHeaders() {
        var exception = new RateLimitExceededException(
                "Too many requests",
                new RateLimitResult(false, 3, 4, 0, 59)
        );

        var response = handler.handleRateLimit(exception, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("3", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("0", response.getHeaders().getFirst("X-RateLimit-Remaining"));
        assertEquals("59", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("Too many requests", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedReturns401() {
        var response = handler.handleUnauthorized(
                new BadCredentialsException("Bad credentials"),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password.", response.getBody().getMessage());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @Test
    void handleAccountLockedReturns423() {
        var response = handler.handleAccountLocked(
                new AccountLockedException("Account locked"),
                request
        );

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        assertEquals("Account locked", response.getBody().getMessage());
        assertEquals("Locked", response.getBody().getError());
    }

    @Test
    void handleDataConflictReturns409() {
        var response = handler.handleDataConflict(
                new DataIntegrityViolationException("Duplicate entry"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("That email or username is already in use.", response.getBody().getMessage());
        assertEquals("Conflict", response.getBody().getError());
    }

    @Test
    void handleDatabaseUnavailableReturns503() {
        var response = handler.handleDatabaseUnavailable(
                new CannotGetJdbcConnectionException("db down"),
                request
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("The service is temporarily unavailable. Please try again shortly.", response.getBody().getMessage());
        assertEquals("Service Unavailable", response.getBody().getError());
        verify(appMetricsService).recordDatabaseUnavailable("request");
    }
}
