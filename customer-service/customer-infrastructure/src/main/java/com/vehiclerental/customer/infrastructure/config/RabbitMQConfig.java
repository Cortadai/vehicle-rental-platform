package com.vehiclerental.customer.infrastructure.config;

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
    public TopicExchange customerExchange() {
        return new TopicExchange("customer.exchange", true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }

    // --- Queues ---

    @Bean
    public Queue customerValidatedQueue() {
        return QueueBuilder.durable("customer.validated.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "customer.validated.dlq")
                .build();
    }

    @Bean
    public Queue customerRejectedQueue() {
        return QueueBuilder.durable("customer.rejected.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "customer.rejected.dlq")
                .build();
    }

    @Bean
    public Queue customerValidateCommandQueue() {
        return QueueBuilder.durable("customer.validate.command.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "customer.validate.command.dlq")
                .build();
    }

    @Bean
    public Queue customerDlq() {
        return QueueBuilder.durable("customer.dlq").build();
    }

    // --- Bindings: queues to customer exchange ---

    @Bean
    public Binding customerValidatedBinding(Queue customerValidatedQueue, TopicExchange customerExchange) {
        return BindingBuilder.bind(customerValidatedQueue).to(customerExchange).with("customer.validated");
    }

    @Bean
    public Binding customerRejectedBinding(Queue customerRejectedQueue, TopicExchange customerExchange) {
        return BindingBuilder.bind(customerRejectedQueue).to(customerExchange).with("customer.rejected");
    }

    @Bean
    public Binding customerValidateCommandBinding(Queue customerValidateCommandQueue, TopicExchange customerExchange) {
        return BindingBuilder.bind(customerValidateCommandQueue).to(customerExchange).with("customer.validate.command");
    }

    // --- DLQ bindings ---

    @Bean
    public Binding customerValidatedDlqBinding(Queue customerDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(customerDlq).to(dlxExchange).with("customer.validated.dlq");
    }

    @Bean
    public Binding customerRejectedDlqBinding(Queue customerDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(customerDlq).to(dlxExchange).with("customer.rejected.dlq");
    }

    @Bean
    public Binding customerValidateCommandDlqBinding(Queue customerDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(customerDlq).to(dlxExchange).with("customer.validate.command.dlq");
    }
}
