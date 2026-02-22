package com.vehiclerental.payment.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }

    // --- Queues ---

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable("payment.completed.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "payment.completed.dlq")
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable("payment.failed.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "payment.failed.dlq")
                .build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable("payment.refunded.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "payment.refunded.dlq")
                .build();
    }

    @Bean
    public Queue paymentDlq() {
        return QueueBuilder.durable("payment.dlq").build();
    }

    // --- Bindings: queues to payment exchange ---

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentCompletedQueue).to(paymentExchange).with("payment.completed");
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(paymentExchange).with("payment.failed");
    }

    @Bean
    public Binding paymentRefundedBinding(Queue paymentRefundedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentRefundedQueue).to(paymentExchange).with("payment.refunded");
    }

    // --- DLQ bindings ---

    @Bean
    public Binding paymentCompletedDlqBinding(Queue paymentDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(paymentDlq).to(dlxExchange).with("payment.completed.dlq");
    }

    @Bean
    public Binding paymentFailedDlqBinding(Queue paymentDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(paymentDlq).to(dlxExchange).with("payment.failed.dlq");
    }

    @Bean
    public Binding paymentRefundedDlqBinding(Queue paymentDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(paymentDlq).to(dlxExchange).with("payment.refunded.dlq");
    }
}
