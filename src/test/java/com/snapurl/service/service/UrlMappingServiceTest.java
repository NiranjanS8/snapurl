package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlMappingServiceTest {

    @Mock
    private UrlMappingRepo urlMappingRepo;
    @Mock
    private ClickEventRepo clickEventRepo;
    @Mock
    private ClickAnalyticsDispatcher clickAnalyticsDispatcher;
    @Mock
    private ShortUrlCacheService shortUrlCacheService;
    @Mock
    private AnalyticsCacheService analyticsCacheService;

    @InjectMocks
    private UrlMappingService urlMappingService;

    private Users user;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("tester");
    }

    @Test
    void createShortUrlRejectsInvalidCustomAliasCharacters() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> urlMappingService.createShortUrl("https://example.com", "bad alias", user)
        );

        assertEquals(UrlMappingService.INVALID_ALIAS_MESSAGE, exception.getMessage());
        verify(urlMappingRepo, never()).save(any());
    }

    @Test
    void createShortUrlRejectsInvalidShortDomains() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> urlMappingService.createShortUrl("a.a", null, user)
        );

        assertEquals(UrlMappingService.INVALID_URL_MESSAGE, exception.getMessage());
        verify(urlMappingRepo, never()).save(any());
    }

    @Test
    void createShortUrlRejectsNumericLeadingHostWithShortTld() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> urlMappingService.createShortUrl("2.ae", null, user)
        );

        assertEquals(UrlMappingService.INVALID_URL_MESSAGE, exception.getMessage());
        verify(urlMappingRepo, never()).save(any());
    }

    @Test
    void createShortUrlRejectsUnknownPublicSuffix() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> urlMappingService.createShortUrl("as.zxzxzxcd", null, user)
        );

        assertEquals(UrlMappingService.INVALID_URL_MESSAGE, exception.getMessage());
        verify(urlMappingRepo, never()).save(any());
    }

    @Test
    void createShortUrlRejectsReservedAlias() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> urlMappingService.createShortUrl("https://example.com", "login", user)
        );

        assertEquals(UrlMappingService.RESERVED_ALIAS_MESSAGE, exception.getMessage());
        verify(urlMappingRepo, never()).save(any());
    }

    @Test
    void createShortUrlRejectsDuplicateAlias() {
        when(urlMappingRepo.existsByShortUrl("my-link")).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> urlMappingService.createShortUrl("https://example.com", "my-link", user)
        );

        assertEquals(UrlMappingService.DUPLICATE_ALIAS_MESSAGE, exception.getMessage());
        verify(urlMappingRepo, never()).save(any());
    }

    @Test
    void createShortUrlWithCustomAliasCachesSavedMapping() {
        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(10L);
        savedMapping.setOriginalUrl("https://example.com");
        savedMapping.setShortUrl("my-link");
        savedMapping.setUser(user);

        when(urlMappingRepo.existsByShortUrl("my-link")).thenReturn(false);
        when(urlMappingRepo.save(any(UrlMapping.class))).thenReturn(savedMapping);

        var result = urlMappingService.createShortUrl("https://example.com", "my-link", user);

        assertEquals("my-link", result.getShortUrl());
        assertEquals("https://example.com", result.getOriginalUrl());
        verify(shortUrlCacheService).put(savedMapping);
    }

    @Test
    void getOriginalUrlReturnsCachedValueBeforeDatabaseLookup() {
        UrlMapping cachedMapping = new UrlMapping();
        cachedMapping.setId(99L);
        cachedMapping.setShortUrl("cached123");
        cachedMapping.setOriginalUrl("https://example.com/cached");

        when(shortUrlCacheService.get("cached123")).thenReturn(cachedMapping);

        UrlMapping result = urlMappingService.getOriginalUrl("cached123");

        assertEquals(cachedMapping, result);
        verify(urlMappingRepo, never()).findByShortUrl("cached123");
    }

    @Test
    void deleteUrlEvictsRedirectAndAnalyticsCaches() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(5L);
        urlMapping.setShortUrl("remove-me");
        urlMapping.setUser(user);

        when(urlMappingRepo.findById(5L)).thenReturn(Optional.of(urlMapping));

        urlMappingService.deleteUrl(5L, user);

        verify(clickEventRepo).deleteByUrlMapping(urlMapping);
        verify(urlMappingRepo).delete(urlMapping);
        verify(shortUrlCacheService).evict("remove-me");
        verify(analyticsCacheService).evictForShortUrl("remove-me");
        verify(analyticsCacheService).evictForUser(1L);
    }
}
