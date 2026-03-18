package com.snapurl.service.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@ConditionalOnMissingBean(RateLimitService.class)
public class NoOpRateLimitService implements RateLimitService {

    @Override
    public boolean isAllowed(String key, long limit, Duration window) {
        return true;
    }
}
