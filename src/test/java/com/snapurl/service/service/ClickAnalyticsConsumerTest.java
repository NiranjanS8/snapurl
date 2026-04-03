package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClickAnalyticsConsumerTest {

    @Mock
    private ClickAnalyticsProcessor clickAnalyticsProcessor;
    @Mock
    private AppMetricsService appMetricsService;

    @InjectMocks
    private ClickAnalyticsConsumer clickAnalyticsConsumer;

    @Test
    void consumeRecordsFailureMetricAndRethrowsWhenProcessingFails() {
        ClickEventMessage message = new ClickEventMessage(1L, 2L, "abc123", LocalDateTime.now());
        RuntimeException failure = new RuntimeException("boom");
        doThrow(failure).when(clickAnalyticsProcessor).processClick(message);

        RuntimeException result = assertThrows(RuntimeException.class, () -> clickAnalyticsConsumer.consume(message));

        verify(appMetricsService).recordAnalyticsProcessingFailure();
        org.junit.jupiter.api.Assertions.assertEquals(failure, result);
    }

    @Test
    void consumeDeadLetterRecordsMetric() {
        ClickEventMessage message = new ClickEventMessage(1L, 2L, "abc123", LocalDateTime.now());
        Message rawMessage = new Message(new byte[0], new MessageProperties());

        clickAnalyticsConsumer.consumeDeadLetter(message, rawMessage);

        verify(appMetricsService).recordAnalyticsDeadLettered();
    }
}
