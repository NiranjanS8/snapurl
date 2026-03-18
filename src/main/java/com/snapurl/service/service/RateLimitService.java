package com.snapurl.service.service;

import java.time.Duration;

public interface RateLimitService {
    boolean isAllowed(String key, long limit, Duration window);
}
