package com.snapurl.service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@AllArgsConstructor
@Slf4j
public class RateLimitGuard {

    private final RateLimitService rateLimitService;
    private final AppMetricsService appMetricsService;

    public RateLimitResult check(String key, long limit, Duration window, String metricName, String message) {
        RateLimitResult result = rateLimitService.check(key, limit, window);
        if (!result.isAllowed()) {
            appMetricsService.recordRateLimitHit(metricName);
            log.warn("Rate limit exceeded metric={} key={}", metricName, key);
            throw new RateLimitExceededException(message, result);
        }
        return result;
    }

    public ResponseEntity.BodyBuilder withHeaders(ResponseEntity.BodyBuilder builder, RateLimitResult result) {
        builder.header("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        builder.header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        if (!result.isAllowed() && result.getRetryAfterSeconds() > 0) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(result.getRetryAfterSeconds()));
        }
        return builder;
    }
}
