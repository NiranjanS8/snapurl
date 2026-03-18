package com.snapurl.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "snapurl.analytics.async-enabled", havingValue = "true")
public class RabbitMqAnalyticsConfig {

    @Bean
    public Declarables clickAnalyticsBindings(
            @Value("${snapurl.rabbitmq.analytics-exchange}") String exchangeName,
            @Value("${snapurl.rabbitmq.click-queue}") String queueName,
            @Value("${snapurl.rabbitmq.click-routing-key}") String routingKey,
            @Value("${snapurl.rabbitmq.click-dlq}") String deadLetterQueueName,
            @Value("${snapurl.rabbitmq.click-dlq-routing-key}") String deadLetterRoutingKey
    ) {
        DirectExchange exchange = new DirectExchange(exchangeName, true, false);
        Queue queue = new Queue(queueName, true, false, false, Map.of(
                "x-dead-letter-exchange", exchangeName,
                "x-dead-letter-routing-key", deadLetterRoutingKey
        ));
        Queue deadLetterQueue = new Queue(deadLetterQueueName, true);

        Binding queueBinding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(exchange).with(deadLetterRoutingKey);

        return new Declarables(exchange, queue, deadLetterQueue, queueBinding, deadLetterBinding);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
