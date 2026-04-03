package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "snapurl.analytics.async-enabled", havingValue = "true")
@Slf4j
public class ClickAnalyticsConsumer {

    private final ClickAnalyticsProcessor clickAnalyticsProcessor;
    private final AppMetricsService appMetricsService;

    @RabbitListener(queues = "${snapurl.rabbitmq.click-queue}")
    public void consume(ClickEventMessage message) {
        try {
            clickAnalyticsProcessor.processClick(message);
        } catch (RuntimeException ex) {
            appMetricsService.recordAnalyticsProcessingFailure();
            log.warn("Analytics event processing failed for shortUrl={} urlMappingId={}, retrying or dead-lettering",
                    message.getShortUrl(), message.getUrlMappingId(), ex);
            throw ex;
        }
    }

    @RabbitListener(queues = "${snapurl.rabbitmq.click-dlq}")
    public void consumeDeadLetter(ClickEventMessage message, Message rawMessage) {
        appMetricsService.recordAnalyticsDeadLettered();
        log.error("Analytics event moved to dead-letter queue shortUrl={} urlMappingId={} xDeath={}",
                message.getShortUrl(),
                message.getUrlMappingId(),
                rawMessage.getMessageProperties().getXDeathHeader());
    }
}
