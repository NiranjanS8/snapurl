package com.snapurl.service.controllers;

import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.models.Users;
import com.snapurl.service.service.RateLimitExceededException;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.RateLimitService;
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
import java.util.Map;

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
    private UserService userService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private Principal principal;

    private UrlMappingController controller;

    @BeforeEach
    void setUp() {
        controller = new UrlMappingController(urlMappingService, userService, rateLimitService);
        ReflectionTestUtils.setField(controller, "publicShortenPerMinute", 3L);
        ReflectionTestUtils.setField(controller, "authShortenPerMinute", 3L);
        ReflectionTestUtils.setField(controller, "trustForwardedHeader", false);
    }

    @Test
    void createPublicShortUrlReturns429WithRateLimitHeadersWhenBlocked() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.check(any(), eq(3L), any())).thenReturn(
                new RateLimitResult(false, 3, 4, 0, 45)
        );

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> controller.createPublicShortUrl(Map.of("originalUrl", "https://example.com"), httpServletRequest)
        );

        assertEquals("Too many public shorten requests. Please try again in a minute.", exception.getMessage());
        assertEquals(3, exception.getRateLimitResult().getLimit());
        assertEquals(45, exception.getRateLimitResult().getRetryAfterSeconds());
    }

    @Test
    void createPublicShortUrlReturnsSuccessWithRateLimitHeaders() {
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setShortUrl("abc123");

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.check(any(), eq(3L), any())).thenReturn(
                new RateLimitResult(true, 3, 1, 2, 0)
        );
        when(urlMappingService.createShortUrl("https://example.com", null, null)).thenReturn(dto);

        var response = controller.createPublicShortUrl(Map.of("originalUrl", "https://example.com"), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("3", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("2", response.getHeaders().getFirst("X-RateLimit-Remaining"));
        assertInstanceOf(UrlMappingDTO.class, response.getBody());
    }

    @Test
    void createShortUrlUsesAuthenticatedPrincipalForRateLimitKey() {
        Users user = new Users();
        user.setEmail("tester@example.com");
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setShortUrl("mine123");

        when(principal.getName()).thenReturn("tester@example.com");
        when(userService.findByEmail("tester@example.com")).thenReturn(user);
        when(rateLimitService.check(eq("snapurl:rate-limit:auth-shorten:tester@example.com"), eq(3L), any())).thenReturn(
                new RateLimitResult(true, 3, 1, 2, 0)
        );
        when(urlMappingService.createShortUrl("https://example.com", null, user)).thenReturn(dto);

        var response = controller.createShortUrl(Map.of("originalUrl", "https://example.com"), principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rateLimitService).check(eq("snapurl:rate-limit:auth-shorten:tester@example.com"), eq(3L), any());
    }
}
