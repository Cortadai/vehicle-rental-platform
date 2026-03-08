package com.vehiclerental.reservation.infrastructure.config;

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

    // --- Reservation exchange and queues (existing) ---

    @Bean
    public TopicExchange reservationExchange() {
        return new TopicExchange("reservation.exchange", true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }

    @Bean
    public Queue reservationCreatedQueue() {
        return QueueBuilder.durable("reservation.created.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "reservation.created.dlq")
                .build();
    }

    @Bean
    public Queue reservationDlq() {
        return QueueBuilder.durable("reservation.dlq").build();
    }

    @Bean
    public Binding reservationCreatedBinding(Queue reservationCreatedQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationCreatedQueue).to(reservationExchange).with("reservation.created");
    }

    @Bean
    public Binding reservationDlqBinding(Queue reservationDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(reservationDlq).to(dlxExchange).with("reservation.created.dlq");
    }

    // --- Participant exchanges (idempotent declarations) ---

    @Bean
    public TopicExchange customerExchange() {
        return new TopicExchange("customer.exchange", true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public TopicExchange fleetExchange() {
        return new TopicExchange("fleet.exchange", true, false);
    }

    // --- SAGA response queues ---

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
    public Queue fleetConfirmedQueue() {
        return QueueBuilder.durable("fleet.confirmed.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "fleet.confirmed.dlq")
                .build();
    }

    @Bean
    public Queue fleetRejectedQueue() {
        return QueueBuilder.durable("fleet.rejected.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "fleet.rejected.dlq")
                .build();
    }

    // --- SAGA response queue bindings ---

    @Bean
    public Binding customerValidatedBinding(Queue customerValidatedQueue, TopicExchange customerExchange) {
        return BindingBuilder.bind(customerValidatedQueue).to(customerExchange).with("customer.validated");
    }

    @Bean
    public Binding customerRejectedBinding(Queue customerRejectedQueue, TopicExchange customerExchange) {
        return BindingBuilder.bind(customerRejectedQueue).to(customerExchange).with("customer.rejected");
    }

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

    @Bean
    public Binding fleetConfirmedBinding(Queue fleetConfirmedQueue, TopicExchange fleetExchange) {
        return BindingBuilder.bind(fleetConfirmedQueue).to(fleetExchange).with("fleet.confirmed");
    }

    @Bean
    public Binding fleetRejectedBinding(Queue fleetRejectedQueue, TopicExchange fleetExchange) {
        return BindingBuilder.bind(fleetRejectedQueue).to(fleetExchange).with("fleet.rejected");
    }
}
