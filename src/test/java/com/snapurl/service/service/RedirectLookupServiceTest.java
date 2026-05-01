package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.repositories.UrlMappingRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectLookupServiceTest {

    @Mock
    private UrlMappingRepo urlMappingRepo;
    @Mock
    private ShortUrlCacheService shortUrlCacheService;
    @Mock
    private ClickAnalyticsDispatcher clickAnalyticsDispatcher;
    @Mock
    private AppMetricsService appMetricsService;

    @InjectMocks
    private RedirectLookupService redirectLookupService;

    @Test
    void getOriginalUrlReturnsCachedValueBeforeDatabaseLookup() {
        UrlMapping cachedMapping = new UrlMapping();
        cachedMapping.setId(99L);
        cachedMapping.setShortUrl("cached123");
        cachedMapping.setOriginalUrl("https://example.com/cached");

        when(shortUrlCacheService.lookup("cached123")).thenReturn(ShortUrlCacheLookupResult.hit(cachedMapping));

        UrlMapping result = redirectLookupService.getOriginalUrl("cached123");

        assertEquals(cachedMapping, result);
        verify(urlMappingRepo, never()).findByShortUrl("cached123");
        verify(appMetricsService).recordRedirectCacheHit();
    }

    @Test
    void getOriginalUrlReturnsNullWhenShortUrlIsCachedAsMissing() {
        when(shortUrlCacheService.lookup("missing123")).thenReturn(ShortUrlCacheLookupResult.knownMissing());

        UrlMapping result = redirectLookupService.getOriginalUrl("missing123");

        assertEquals(null, result);
        verify(urlMappingRepo, never()).findByShortUrl("missing123");
        verify(appMetricsService).recordRedirectNegativeCacheHit();
    }

    @Test
    void getOriginalUrlCachesKnownMissingResultAfterDatabaseMiss() {
        when(shortUrlCacheService.lookup("missing123")).thenReturn(ShortUrlCacheLookupResult.miss());
        when(urlMappingRepo.findByShortUrl("missing123")).thenReturn(null);

        UrlMapping result = redirectLookupService.getOriginalUrl("missing123");

        assertEquals(null, result);
        verify(appMetricsService).recordRedirectCacheMiss();
        verify(appMetricsService).recordRedirectDatabaseLookup(false);
        verify(shortUrlCacheService).putMissing("missing123");
    }

    @Test
    void trackRedirectDispatchesClickAnalytics() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(7L);
        urlMapping.setShortUrl("abc123");

        redirectLookupService.trackRedirect(urlMapping);

        verify(clickAnalyticsDispatcher).dispatchClick(urlMapping);
    }
}
