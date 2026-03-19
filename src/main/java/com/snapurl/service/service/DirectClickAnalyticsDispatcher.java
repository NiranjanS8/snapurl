package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.UrlMapping;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "snapurl.analytics.async-enabled", havingValue = "false", matchIfMissing = true)
public class DirectClickAnalyticsDispatcher implements ClickAnalyticsDispatcher {

    private final ClickAnalyticsProcessor clickAnalyticsProcessor;

    @Override
    @Async
    public void dispatchClick(UrlMapping urlMapping) {
        clickAnalyticsProcessor.processClick(new ClickEventMessage(
                urlMapping.getId(),
                urlMapping.getUser() != null ? urlMapping.getUser().getId() : null,
                urlMapping.getShortUrl(),
                LocalDateTime.now()
        ));
    }
}
