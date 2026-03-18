package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.UrlMapping;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "snapurl.analytics.async-enabled", havingValue = "false", matchIfMissing = true)
public class DirectClickAnalyticsDispatcher implements ClickAnalyticsDispatcher {

    private final ClickAnalyticsProcessor clickAnalyticsProcessor;

    @Override
    public void dispatchClick(UrlMapping urlMapping) {
        clickAnalyticsProcessor.processClick(new ClickEventMessage(
                urlMapping.getId(),
                urlMapping.getShortUrl(),
                LocalDateTime.now()
        ));
    }
}
