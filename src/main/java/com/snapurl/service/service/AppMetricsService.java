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

    public void recordAnalyticsPublished() {
        meterRegistry.counter("snapurl.analytics.events.published").increment();
    }

    public void recordAnalyticsPublishFallback() {
        meterRegistry.counter("snapurl.analytics.events.publish_fallback").increment();
    }

    public void recordAnalyticsProcessed() {
        meterRegistry.counter("snapurl.analytics.events.processed").increment();
    }

    public void recordAnalyticsProcessingFailure() {
        meterRegistry.counter("snapurl.analytics.events.processing_failures").increment();
    }

    public void recordAnalyticsDeadLettered() {
        meterRegistry.counter("snapurl.analytics.events.dead_lettered").increment();
    }

    public void recordAnalyticsDuplicateSkipped() {
        meterRegistry.counter("snapurl.analytics.events.duplicates_skipped").increment();
    }

    public void recordRedisFailure(String component, String operation) {
        meterRegistry.counter("snapurl.redis.failures", "component", component, "operation", operation).increment();
    }

    public void recordRateLimitFallback(boolean failOpen) {
        meterRegistry.counter("snapurl.rate.limit.fallbacks", "mode", failOpen ? "fail_open" : "fail_closed").increment();
    }

    public void recordDatabaseUnavailable(String operation) {
        meterRegistry.counter("snapurl.database.unavailable", "operation", operation).increment();
    }

    public void recordLoginFailure() {
        meterRegistry.counter("snapurl.login.failures").increment();
    }

    public void recordRateLimitHit(String endpoint) {
        meterRegistry.counter("snapurl.rate.limit.hits", "endpoint", endpoint).increment();
    }
}
