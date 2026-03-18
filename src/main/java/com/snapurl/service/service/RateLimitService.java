package com.snapurl.service.service;

import java.time.Duration;

public interface RateLimitService {
    RateLimitResult check(String key, long limit, Duration window);
}
