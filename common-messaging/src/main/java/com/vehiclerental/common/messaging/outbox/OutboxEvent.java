package com.vehiclerental.common.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_events_status_created", columnList = "status, created_at")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;

    @Column(name = "exchange", nullable = false, length = 100)
    private String exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(String aggregateType, String aggregateId, String eventType,
                        String payload, String routingKey, String exchange) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.routingKey = routingKey;
        this.exchange = exchange;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
        this.publishedAt = null;
    }

    public static OutboxEvent create(String aggregateType, String aggregateId, String eventType,
                                     String payload, String routingKey, String exchange) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, payload, routingKey, exchange);
    }

    public Long getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getExchange() {
        return exchange;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
