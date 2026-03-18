package com.snapurl.service.controllers;

import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
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
}
