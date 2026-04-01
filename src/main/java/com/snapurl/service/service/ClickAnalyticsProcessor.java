package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.ClickEvent;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ClickAnalyticsProcessor {

    private final UrlMappingRepo urlMappingRepo;
    private final ClickEventRepo clickEventRepo;
    private final AnalyticsCacheService analyticsCacheService;

    @Transactional
    public void processClick(ClickEventMessage message) {
        int updatedRows = urlMappingRepo.incrementClickCountAndUpdateLastAccessed(
                message.getUrlMappingId(),
                message.getClickedAt()
        );
        if (updatedRows == 0) {
            log.warn("Analytics event skipped because url mapping was not found urlMappingId={} shortUrl={}",
                    message.getUrlMappingId(), message.getShortUrl());
            return;
        }

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setUrlMapping(urlMappingRepo.getReferenceById(message.getUrlMappingId()));
        clickEvent.setClickTime(message.getClickedAt());
        clickEventRepo.save(clickEvent);

        analyticsCacheService.evictForShortUrl(message.getShortUrl());
        if (message.getUserId() != null) {
            analyticsCacheService.evictForUser(message.getUserId());
        }
        log.info("Analytics event processed for shortUrl={} userId={}", message.getShortUrl(), message.getUserId());
    }
}
