package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.ClickEvent;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickAnalyticsProcessorTest {

    @Mock
    private UrlMappingRepo urlMappingRepo;
    @Mock
    private ClickEventRepo clickEventRepo;
    @Mock
    private AnalyticsCacheService analyticsCacheService;
    @Mock
    private AppMetricsService appMetricsService;

    @InjectMocks
    private ClickAnalyticsProcessor clickAnalyticsProcessor;

    @Test
    void processClickSkipsDuplicateEventWithoutDoubleCounting() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(10L);

        ClickEventMessage message = new ClickEventMessage("evt-1", 10L, 5L, "abc123", LocalDateTime.now());

        when(urlMappingRepo.findById(10L)).thenReturn(Optional.of(urlMapping));
        when(clickEventRepo.saveAndFlush(any(ClickEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        clickAnalyticsProcessor.processClick(message);

        verify(appMetricsService).recordAnalyticsDuplicateSkipped();
        verify(urlMappingRepo, never()).incrementClickCountAndUpdateLastAccessed(any(), any());
        verify(analyticsCacheService, never()).evictForShortUrl(any());
    }

    @Test
    void processClickRequiresStableEventId() {
        ClickEventMessage message = new ClickEventMessage("", 10L, 5L, "abc123", LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> clickAnalyticsProcessor.processClick(message));

        verify(clickEventRepo, never()).saveAndFlush(any());
    }

    @Test
    void processClickPersistsEventBeforeIncrementingCount() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(10L);

        ClickEventMessage message = new ClickEventMessage("evt-1", 10L, 5L, "abc123", LocalDateTime.now());

        when(urlMappingRepo.findById(10L)).thenReturn(Optional.of(urlMapping));
        when(urlMappingRepo.incrementClickCountAndUpdateLastAccessed(10L, message.getClickedAt())).thenReturn(1);

        clickAnalyticsProcessor.processClick(message);

        verify(clickEventRepo).saveAndFlush(any(ClickEvent.class));
        verify(urlMappingRepo).incrementClickCountAndUpdateLastAccessed(10L, message.getClickedAt());
        verify(appMetricsService).recordAnalyticsProcessed();
    }
}
