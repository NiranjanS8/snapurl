package com.snapurl.service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitGuardTest {

    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private AppMetricsService appMetricsService;

    @Test
    void checkReturnsAllowedResult() {
        RateLimitGuard guard = new RateLimitGuard(rateLimitService, appMetricsService);
        RateLimitResult allowed = new RateLimitResult(true, 5, 1, 4, 0);
        when(rateLimitService.check("key", 5, Duration.ofMinutes(1))).thenReturn(allowed);

        RateLimitResult result = guard.check("key", 5, Duration.ofMinutes(1), "metric", "blocked");

        assertEquals(allowed, result);
    }

    @Test
    void checkRecordsMetricAndThrowsWhenBlocked() {
        RateLimitGuard guard = new RateLimitGuard(rateLimitService, appMetricsService);
        RateLimitResult blocked = new RateLimitResult(false, 5, 6, 0, 30);
        when(rateLimitService.check("key", 5, Duration.ofMinutes(1))).thenReturn(blocked);

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> guard.check("key", 5, Duration.ofMinutes(1), "metric", "blocked")
        );

        assertEquals("blocked", exception.getMessage());
        assertEquals(blocked, exception.getRateLimitResult());
        verify(appMetricsService).recordRateLimitHit("metric");
    }

    @Test
    void withHeadersAddsRateLimitHeadersAndRetryAfter() {
        RateLimitGuard guard = new RateLimitGuard(rateLimitService, appMetricsService);
        RateLimitResult blocked = new RateLimitResult(false, 5, 6, 0, 30);

        ResponseEntity<?> response = guard.withHeaders(ResponseEntity.status(429), blocked).build();

        assertEquals("5", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("0", response.getHeaders().getFirst("X-RateLimit-Remaining"));
        assertEquals("30", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }
}
