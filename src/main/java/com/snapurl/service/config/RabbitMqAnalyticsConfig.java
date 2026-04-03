package com.snapurl.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
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

    @Bean
    public StatelessRetryOperationsInterceptor clickAnalyticsRetryInterceptor(
            @Value("${snapurl.rabbitmq.listener-max-attempts:3}") int maxAttempts,
            @Value("${snapurl.rabbitmq.listener-initial-interval-ms:1000}") long initialInterval,
            @Value("${snapurl.rabbitmq.listener-multiplier:2.0}") double multiplier,
            @Value("${snapurl.rabbitmq.listener-max-interval-ms:5000}") long maxInterval
    ) {
        return RetryInterceptorBuilder
                .stateless()
                .maxRetries(maxAttempts)
                .backOffOptions(initialInterval, multiplier, maxInterval)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            StatelessRetryOperationsInterceptor clickAnalyticsRetryInterceptor,
            @Value("${snapurl.rabbitmq.listener-recovery-interval-ms:5000}") long recoveryIntervalMs
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        factory.setRecoveryInterval(recoveryIntervalMs);
        factory.setAdviceChain(clickAnalyticsRetryInterceptor);
        return factory;
    }
}
