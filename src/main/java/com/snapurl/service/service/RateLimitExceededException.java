package com.snapurl.service.service;

public class RateLimitExceededException extends RuntimeException {

    private final RateLimitResult rateLimitResult;

    public RateLimitExceededException(String message, RateLimitResult rateLimitResult) {
        super(message);
        this.rateLimitResult = rateLimitResult;
    }

    public RateLimitResult getRateLimitResult() {
        return rateLimitResult;
    }
}
