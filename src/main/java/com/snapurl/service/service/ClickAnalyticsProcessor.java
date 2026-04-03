package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.ClickEvent;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ClickAnalyticsProcessor {

    private final UrlMappingRepo urlMappingRepo;
    private final ClickEventRepo clickEventRepo;
    private final AnalyticsCacheService analyticsCacheService;
    private final AppMetricsService appMetricsService;

    @Transactional
    public void processClick(ClickEventMessage message) {
        if (message.getEventId() == null || message.getEventId().isBlank()) {
            throw new IllegalArgumentException("Analytics eventId is required");
        }

        UrlMapping urlMapping = urlMappingRepo.findById(message.getUrlMappingId()).orElse(null);
        if (urlMapping == null) {
            log.warn("Analytics event skipped because url mapping was not found urlMappingId={} shortUrl={}",
                    message.getUrlMappingId(), message.getShortUrl());
            return;
        }

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setEventId(message.getEventId());
        clickEvent.setUrlMapping(urlMapping);
        clickEvent.setClickTime(message.getClickedAt());

        try {
            clickEventRepo.saveAndFlush(clickEvent);
        } catch (DataIntegrityViolationException ex) {
            appMetricsService.recordAnalyticsDuplicateSkipped();
            log.info("Duplicate analytics event skipped eventId={} shortUrl={}", message.getEventId(), message.getShortUrl());
            return;
        }

        int updatedRows = urlMappingRepo.incrementClickCountAndUpdateLastAccessed(
                message.getUrlMappingId(),
                message.getClickedAt()
        );
        if (updatedRows == 0) {
            throw new IllegalStateException("Analytics event persisted but click count update failed for urlMappingId=" + message.getUrlMappingId());
        }

        analyticsCacheService.evictForShortUrl(message.getShortUrl());
        if (message.getUserId() != null) {
            analyticsCacheService.evictForUser(message.getUserId());
        }
        appMetricsService.recordAnalyticsProcessed();
        log.info("Analytics event processed for shortUrl={} userId={}", message.getShortUrl(), message.getUserId());
    }
}
