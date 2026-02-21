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
}
