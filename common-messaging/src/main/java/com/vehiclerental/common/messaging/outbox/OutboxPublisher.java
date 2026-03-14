package com.vehiclerental.common.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RabbitTemplate rabbitTemplate,
                           TransactionTemplate transactionTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            transactionTemplate.executeWithoutResult(status -> processEvent(event));
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            var messageBuilder = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setHeader("X-Aggregate-Type", event.getAggregateType())
                    .setHeader("X-Aggregate-Id", event.getAggregateId())
                    .setMessageId(String.valueOf(event.getId()));

            if (event.getTraceParent() != null) {
                messageBuilder.setHeader("traceparent", event.getTraceParent());
            }

            Message message = messageBuilder.build();

            rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message);

            event.markPublished();
            outboxEventRepository.save(event);

            log.debug("Published outbox event [id={}, type={}, aggregateId={}]",
                    event.getId(), event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            event.incrementRetryCount();
            if (event.getRetryCount() >= MAX_RETRIES) {
                event.markFailed();
                log.error("Outbox event FAILED after {} retries [id={}, type={}, aggregateId={}]",
                        MAX_RETRIES, event.getId(), event.getEventType(), event.getAggregateId(), e);
            } else {
                log.warn("Failed to publish outbox event [id={}, type={}, retry={}]: {}",
                        event.getId(), event.getEventType(), event.getRetryCount(), e.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }
}
