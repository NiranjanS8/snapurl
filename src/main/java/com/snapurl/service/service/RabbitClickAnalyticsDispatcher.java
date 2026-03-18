package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.UrlMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@ConditionalOnProperty(name = "snapurl.analytics.async-enabled", havingValue = "true")
public class RabbitClickAnalyticsDispatcher implements ClickAnalyticsDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final ClickAnalyticsProcessor clickAnalyticsProcessor;
    private final String exchangeName;
    private final String routingKey;

    public RabbitClickAnalyticsDispatcher(
            RabbitTemplate rabbitTemplate,
            ClickAnalyticsProcessor clickAnalyticsProcessor,
            @Value("${snapurl.rabbitmq.analytics-exchange}") String exchangeName,
            @Value("${snapurl.rabbitmq.click-routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.clickAnalyticsProcessor = clickAnalyticsProcessor;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    @Async
    public void dispatchClick(UrlMapping urlMapping) {
        ClickEventMessage message = new ClickEventMessage(
                urlMapping.getId(),
                urlMapping.getShortUrl(),
                LocalDateTime.now()
        );

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        } catch (RuntimeException ex) {
            log.warn("RabbitMQ publish failed for shortUrl={}, falling back to direct processing", urlMapping.getShortUrl(), ex);
            clickAnalyticsProcessor.processClick(message);
        }
    }
}
