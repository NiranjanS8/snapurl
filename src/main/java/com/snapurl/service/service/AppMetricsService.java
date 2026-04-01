package com.snapurl.service.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class AppMetricsService {

    private final MeterRegistry meterRegistry;

    public AppMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLinkCreated(String accessType) {
        meterRegistry.counter("snapurl.links.created", "access_type", accessType).increment();
    }

    public void recordRedirectResolved() {
        meterRegistry.counter("snapurl.redirects.resolved").increment();
    }

    public void recordLoginFailure() {
        meterRegistry.counter("snapurl.login.failures").increment();
    }

    public void recordRateLimitHit(String endpoint) {
        meterRegistry.counter("snapurl.rate.limit.hits", "endpoint", endpoint).increment();
    }
}
