package com.vehiclerental.fleet.infrastructure.config;

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
    public TopicExchange fleetExchange() {
        return new TopicExchange("fleet.exchange", true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }

    // --- Event queues ---

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

    // --- Command queues ---

    @Bean
    public Queue fleetConfirmCommandQueue() {
        return QueueBuilder.durable("fleet.confirm.command.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "fleet.confirm.command.dlq")
                .build();
    }

    @Bean
    public Queue fleetReleaseCommandQueue() {
        return QueueBuilder.durable("fleet.release.command.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "fleet.release.command.dlq")
                .build();
    }

    // --- DLQ ---

    @Bean
    public Queue fleetDlq() {
        return QueueBuilder.durable("fleet.dlq").build();
    }

    // --- Bindings: queues to fleet exchange ---

    @Bean
    public Binding fleetConfirmedBinding(Queue fleetConfirmedQueue, TopicExchange fleetExchange) {
        return BindingBuilder.bind(fleetConfirmedQueue).to(fleetExchange).with("fleet.confirmed");
    }

    @Bean
    public Binding fleetRejectedBinding(Queue fleetRejectedQueue, TopicExchange fleetExchange) {
        return BindingBuilder.bind(fleetRejectedQueue).to(fleetExchange).with("fleet.rejected");
    }

    @Bean
    public Binding fleetConfirmCommandBinding(Queue fleetConfirmCommandQueue, TopicExchange fleetExchange) {
        return BindingBuilder.bind(fleetConfirmCommandQueue).to(fleetExchange).with("fleet.confirm.command");
    }

    @Bean
    public Binding fleetReleaseCommandBinding(Queue fleetReleaseCommandQueue, TopicExchange fleetExchange) {
        return BindingBuilder.bind(fleetReleaseCommandQueue).to(fleetExchange).with("fleet.release.command");
    }

    // --- DLQ bindings ---

    @Bean
    public Binding fleetConfirmedDlqBinding(Queue fleetDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(fleetDlq).to(dlxExchange).with("fleet.confirmed.dlq");
    }

    @Bean
    public Binding fleetRejectedDlqBinding(Queue fleetDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(fleetDlq).to(dlxExchange).with("fleet.rejected.dlq");
    }

    @Bean
    public Binding fleetConfirmCommandDlqBinding(Queue fleetDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(fleetDlq).to(dlxExchange).with("fleet.confirm.command.dlq");
    }

    @Bean
    public Binding fleetReleaseCommandDlqBinding(Queue fleetDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(fleetDlq).to(dlxExchange).with("fleet.release.command.dlq");
    }
}
