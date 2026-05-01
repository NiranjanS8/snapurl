package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ShortenUrlRequest;
import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.models.Users;
import com.snapurl.service.service.ClientIpResolver;
import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitGuard;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.AppMetricsService;
import com.snapurl.service.service.UrlAnalyticsService;
import com.snapurl.service.service.UrlMappingService;
import com.snapurl.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlMappingControllerTest {

    @Mock
    private UrlMappingService urlMappingService;
    @Mock
    private UrlAnalyticsService urlAnalyticsService;
    @Mock
    private UserService userService;
    @Mock
    private RateLimitGuard rateLimitGuard;
    @Mock
    private ClientIpResolver clientIpResolver;
    @Mock
    private AppMetricsService appMetricsService;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private Principal principal;

    private UrlMappingController controller;

    @BeforeEach
    void setUp() {
        controller = new UrlMappingController(urlMappingService, urlAnalyticsService, userService, rateLimitGuard, clientIpResolver, appMetricsService);
        ReflectionTestUtils.setField(controller, "publicShortenPerMinute", 3L);
        ReflectionTestUtils.setField(controller, "authShortenPerMinute", 3L);
    }

    @Test
    void createPublicShortUrlReturns429WithRateLimitHeadersWhenBlocked() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(
                eq("snapurl:rate-limit:public-shorten:127.0.0.1"),
                eq(3L),
                any(),
                eq("urls_public_shorten"),
                eq("Too many public shorten requests. Please try again in a minute.")
        )).thenThrow(new RateLimitExceededException(
                "Too many public shorten requests. Please try again in a minute.",
                new RateLimitResult(false, 3, 4, 0, 45)
        ));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> controller.createPublicShortUrl(request, httpServletRequest)
        );

        assertEquals("Too many public shorten requests. Please try again in a minute.", exception.getMessage());
        assertEquals(3, exception.getRateLimitResult().getLimit());
        assertEquals(45, exception.getRateLimitResult().getRetryAfterSeconds());
    }

    @Test
    void createPublicShortUrlReturnsSuccessWithRateLimitHeaders() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");

        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setShortUrl("abc123");

        when(clientIpResolver.resolve(httpServletRequest)).thenReturn("127.0.0.1");
        when(rateLimitGuard.check(eq("snapurl:rate-limit:public-shorten:127.0.0.1"), eq(3L), any(), eq("urls_public_shorten"), any())).thenReturn(
                new RateLimitResult(true, 3, 1, 2, 0)
        );
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();
        when(urlMappingService.createShortUrl("https://example.com", null, null)).thenReturn(dto);

        var response = controller.createPublicShortUrl(request, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("3", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("2", response.getHeaders().getFirst("X-RateLimit-Remaining"));
        assertInstanceOf(UrlMappingDTO.class, response.getBody());
    }

    @Test
    void createShortUrlUsesAuthenticatedPrincipalForRateLimitKey() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://example.com");

        Users user = new Users();
        user.setEmail("tester@example.com");
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setShortUrl("mine123");

        when(principal.getName()).thenReturn("tester@example.com");
        when(userService.findByEmail("tester@example.com")).thenReturn(user);
        when(rateLimitGuard.check(eq("snapurl:rate-limit:auth-shorten:tester@example.com"), eq(3L), any(), eq("urls_authenticated_shorten"), any())).thenReturn(
                new RateLimitResult(true, 3, 1, 2, 0)
        );
        when(rateLimitGuard.withHeaders(any(), any())).thenCallRealMethod();
        when(urlMappingService.createShortUrl("https://example.com", null, user)).thenReturn(dto);

        var response = controller.createShortUrl(request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rateLimitGuard).check(eq("snapurl:rate-limit:auth-shorten:tester@example.com"), eq(3L), any(), eq("urls_authenticated_shorten"), any());
    }

    @Test
    void getUrlAnalyticsUsesAuthenticatedUsersOwnershipContext() {
        Users user = new Users();
        user.setId(10L);
        user.setEmail("tester@example.com");

        when(principal.getName()).thenReturn("tester@example.com");
        when(userService.findByEmail("tester@example.com")).thenReturn(user);
        when(urlAnalyticsService.getClickEventByDate(eq("mine123"), any(LocalDateTime.class), any(LocalDateTime.class), eq(user)))
                .thenReturn(List.of());

        var response = controller.getUrlAnalytics("mine123", "2026-04-01T00:00:00", "2026-04-01T23:59:59", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).findByEmail("tester@example.com");
        verify(urlAnalyticsService).getClickEventByDate(eq("mine123"), any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
    }
}
