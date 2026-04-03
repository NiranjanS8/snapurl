package com.snapurl.service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = {"snapurl.redis.enabled", "snapurl.rate-limit.enabled"}, havingValue = "true")
public class RedisRateLimitService implements RateLimitService {

    private static final String RATE_LIMIT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {current, ttl}
            """;

    private final StringRedisTemplate redisTemplate;
    private final boolean failOpen;
    private final DefaultRedisScript<List> rateLimitScript;
    private final AppMetricsService appMetricsService;

    public RedisRateLimitService(
            StringRedisTemplate redisTemplate,
            AppMetricsService appMetricsService,
            @org.springframework.beans.factory.annotation.Value("${snapurl.rate-limit.fail-open:true}") boolean failOpen
    ) {
        this.redisTemplate = redisTemplate;
        this.appMetricsService = appMetricsService;
        this.failOpen = failOpen;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(RATE_LIMIT_SCRIPT);
        this.rateLimitScript.setResultType(List.class);
    }

    @Override
    public RateLimitResult check(String key, long limit, Duration window) {
        try {
            List result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(limit),
                    String.valueOf(window.getSeconds())
            );

            if (result == null || result.size() < 2) {
                return fallback(limit);
            }

            long currentCount = ((Number) result.get(0)).longValue();
            long ttlSeconds = Math.max(((Number) result.get(1)).longValue(), 0L);
            boolean allowed = currentCount <= limit;
            long remaining = Math.max(limit - currentCount, 0L);

            return new RateLimitResult(allowed, limit, currentCount, remaining, allowed ? 0L : ttlSeconds);
        } catch (RuntimeException ex) {
            log.warn("Rate limit check failed for key={}", key, ex);
            appMetricsService.recordRedisFailure("rate_limit", "check");
            return fallback(limit);
        }
    }

    private RateLimitResult fallback(long limit) {
        appMetricsService.recordRateLimitFallback(failOpen);
        return new RateLimitResult(failOpen, limit, 0L, limit, 0L);
    }
}
