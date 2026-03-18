package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "snapurl.analytics.async-enabled", havingValue = "true")
public class ClickAnalyticsConsumer {

    private final ClickAnalyticsProcessor clickAnalyticsProcessor;

    @RabbitListener(queues = "${snapurl.rabbitmq.click-queue}")
    public void consume(ClickEventMessage message) {
        clickAnalyticsProcessor.processClick(message);
    }
}
