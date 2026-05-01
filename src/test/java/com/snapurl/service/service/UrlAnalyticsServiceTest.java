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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlAnalyticsServiceTest {

    @Mock
    private UrlMappingRepo urlMappingRepo;
    @Mock
    private ClickEventRepo clickEventRepo;
    @Mock
    private AnalyticsCacheService analyticsCacheService;

    @InjectMocks
    private UrlAnalyticsService urlAnalyticsService;

    private Users user;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("tester");
    }

    @Test
    void getClickEventByDateRejectsAccessForDifferentOwner() {
        Users owner = new Users();
        owner.setId(99L);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(50L);
        urlMapping.setShortUrl("private123");
        urlMapping.setUser(owner);

        when(analyticsCacheService.getUrlAnalytics(any(), any(), any())).thenReturn(null);
        when(urlMappingRepo.findByShortUrl("private123")).thenReturn(urlMapping);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> urlAnalyticsService.getClickEventByDate(
                        "private123",
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now(),
                        user
                )
        );

        assertEquals("You can only view analytics for your own short links", exception.getMessage());
    }

    @Test
    void getClickEventByDateUsesAggregatedRepositoryQuery() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 30, 23, 59);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(50L);
        urlMapping.setShortUrl("stats123");
        urlMapping.setUser(user);

        when(analyticsCacheService.getUrlAnalytics("stats123", start, end)).thenReturn(null);
        when(urlMappingRepo.findByShortUrl("stats123")).thenReturn(urlMapping);
        when(clickEventRepo.countByUrlMappingGroupedByDate(urlMapping, start, end))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 4, 10), 3L}));

        var result = urlAnalyticsService.getClickEventByDate("stats123", start, end, user);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 4, 10), result.getFirst().getClickDate());
        assertEquals(3L, result.getFirst().getClickCount());
        verify(clickEventRepo).countByUrlMappingGroupedByDate(urlMapping, start, end);
        verify(analyticsCacheService).putUrlAnalytics("stats123", start, end, result);
    }

    @Test
    void getTotalClicksByUserAndDateUsesAggregatedRepositoryQuery() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        UrlMapping first = new UrlMapping();
        first.setId(1L);
        first.setUser(user);
        UrlMapping second = new UrlMapping();
        second.setId(2L);
        second.setUser(user);
        List<UrlMapping> mappings = List.of(first, second);

        when(analyticsCacheService.getTotalClicks(1L, start, end)).thenReturn(null);
        when(urlMappingRepo.findByUser(user)).thenReturn(mappings);
        when(clickEventRepo.countByUrlMappingsGroupedByDate(eq(mappings), eq(start.atStartOfDay()), eq(end.plusDays(1).atStartOfDay())))
                .thenReturn(List.<Object[]>of(
                        new Object[]{LocalDate.of(2026, 4, 10), 3L},
                        new Object[]{LocalDate.of(2026, 4, 11), 5L}
                ));

        Map<LocalDate, Long> result = urlAnalyticsService.getTotalClicksByUserAndDate(user, start, end);

        assertEquals(2, result.size());
        assertEquals(3L, result.get(LocalDate.of(2026, 4, 10)));
        assertEquals(5L, result.get(LocalDate.of(2026, 4, 11)));
        verify(clickEventRepo).countByUrlMappingsGroupedByDate(mappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        verify(analyticsCacheService).putTotalClicks(1L, start, end, result);
    }

    @Test
    void getTotalClicksByUserAndDateReturnsEmptyMapWhenUserHasNoUrls() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        when(analyticsCacheService.getTotalClicks(1L, start, end)).thenReturn(null);
        when(urlMappingRepo.findByUser(user)).thenReturn(List.of());

        Map<LocalDate, Long> result = urlAnalyticsService.getTotalClicksByUserAndDate(user, start, end);

        assertEquals(0, result.size());
        verify(clickEventRepo, never()).countByUrlMappingsGroupedByDate(any(), any(), any());
    }
}
