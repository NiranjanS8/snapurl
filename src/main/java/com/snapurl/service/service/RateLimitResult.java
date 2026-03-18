package com.snapurl.service.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RateLimitResult {
    private boolean allowed;
    private long limit;
    private long currentCount;
    private long remaining;
    private long retryAfterSeconds;
}
