package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RabbitClickAnalyticsDispatcherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ClickAnalyticsProcessor clickAnalyticsProcessor;
    @Mock
    private AppMetricsService appMetricsService;

    @Test
    void dispatchClickRecordsPublishedMetricWhenRabbitSendSucceeds() {
        RabbitClickAnalyticsDispatcher rabbitClickAnalyticsDispatcher = new RabbitClickAnalyticsDispatcher(
                rabbitTemplate,
                clickAnalyticsProcessor,
                appMetricsService,
                "snapurl.analytics",
                "click.created"
        );

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(1L);
        urlMapping.setShortUrl("abc123");

        rabbitClickAnalyticsDispatcher.dispatchClick(urlMapping);

        verify(rabbitTemplate).convertAndSend(eq("snapurl.analytics"), eq("click.created"), any(Object.class));
        verify(appMetricsService).recordAnalyticsPublished();
    }

    @Test
    void dispatchClickFallsBackToDirectProcessingWhenRabbitSendFails() {
        RabbitClickAnalyticsDispatcher rabbitClickAnalyticsDispatcher = new RabbitClickAnalyticsDispatcher(
                rabbitTemplate,
                clickAnalyticsProcessor,
                appMetricsService,
                "snapurl.analytics",
                "click.created"
        );

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(1L);
        urlMapping.setShortUrl("abc123");

        doThrow(new RuntimeException("rabbit down"))
                .when(rabbitTemplate)
                .convertAndSend(eq("snapurl.analytics"), eq("click.created"), any(Object.class));

        rabbitClickAnalyticsDispatcher.dispatchClick(urlMapping);

        verify(appMetricsService).recordAnalyticsPublishFallback();
        verify(clickAnalyticsProcessor).processClick(any());
    }
}
