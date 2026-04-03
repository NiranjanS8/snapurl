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

    public void recordRedirectCacheHit() {
        meterRegistry.counter("snapurl.redirect.cache.lookups", "result", "hit").increment();
    }

    public void recordRedirectCacheMiss() {
        meterRegistry.counter("snapurl.redirect.cache.lookups", "result", "miss").increment();
    }

    public void recordRedirectNegativeCacheHit() {
        meterRegistry.counter("snapurl.redirect.cache.lookups", "result", "known_missing").increment();
    }

    public void recordRedirectDatabaseLookup(boolean found) {
        meterRegistry.counter("snapurl.redirect.db.lookups", "result", found ? "found" : "not_found").increment();
    }

    public void recordLoginFailure() {
        meterRegistry.counter("snapurl.login.failures").increment();
    }

    public void recordRateLimitHit(String endpoint) {
        meterRegistry.counter("snapurl.rate.limit.hits", "endpoint", endpoint).increment();
    }
}
