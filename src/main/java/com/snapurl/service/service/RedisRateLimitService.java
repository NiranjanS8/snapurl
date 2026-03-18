package com.snapurl.service.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@ConditionalOnProperty(name = {"snapurl.redis.enabled", "snapurl.rate-limit.enabled"}, havingValue = "true")
public class RedisRateLimitService implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, long limit, Duration window) {
        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == null) {
            return true;
        }

        if (currentCount == 1L) {
            redisTemplate.expire(key, window);
        }

        return currentCount <= limit;
    }
}
