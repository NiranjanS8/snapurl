package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class RedirectLookupService {

    private final UrlMappingRepo urlMappingRepo;
    private final ShortUrlCacheService shortUrlCacheService;
    private final ClickAnalyticsDispatcher clickAnalyticsDispatcher;
    private final AppMetricsService appMetricsService;

    public UrlMapping getOriginalUrl(String shortUrl) {
        ShortUrlCacheLookupResult cacheLookupResult = shortUrlCacheService.lookup(shortUrl);
        if (cacheLookupResult.isHit()) {
            appMetricsService.recordRedirectCacheHit();
            UrlMapping cached = cacheLookupResult.getUrlMapping();
            if (isExpired(cached)) {
                log.debug("Cached redirect is expired for shortUrl={}", shortUrl);
                return null;
            }
            return cached;
        }

        if (cacheLookupResult.isKnownMissing()) {
            appMetricsService.recordRedirectNegativeCacheHit();
            return null;
        }

        appMetricsService.recordRedirectCacheMiss();
        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        appMetricsService.recordRedirectDatabaseLookup(urlMapping != null);
        if (urlMapping != null) {
            if (isExpired(urlMapping)) {
                log.debug("Database redirect is expired for shortUrl={}", shortUrl);
                return null;
            }
            shortUrlCacheService.put(urlMapping);
            log.debug("Redirect cache populated from database for shortUrl={}", shortUrl);
        } else {
            shortUrlCacheService.putMissing(shortUrl);
            log.debug("Redirect cache stored known-missing marker for shortUrl={}", shortUrl);
        }
        return urlMapping;
    }

    public void trackRedirect(UrlMapping urlMapping) {
        if (urlMapping == null) {
            return;
        }
        clickAnalyticsDispatcher.dispatchClick(urlMapping);
    }

    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiresAt() != null && !urlMapping.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
