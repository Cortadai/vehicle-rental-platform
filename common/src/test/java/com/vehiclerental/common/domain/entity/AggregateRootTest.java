package com.vehiclerental.common.domain.entity;

import com.vehiclerental.common.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateRootTest {

    private static class TestAggregate extends AggregateRoot<UUID> {
        TestAggregate(UUID id) {
            super(id);
        }

        void addEvent(DomainEvent event) {
            registerDomainEvent(event);
        }
    }

    private record TestEvent(UUID eventId, Instant occurredOn) implements DomainEvent {
        TestEvent {
            if (eventId == null) throw new NullPointerException("eventId must not be null");
            if (occurredOn == null) throw new NullPointerException("occurredOn must not be null");
        }
    }

    @Test
    void registerSingleEvent_shouldAppearInGetDomainEvents() {
        var aggregate = new TestAggregate(UUID.randomUUID());
        var event = new TestEvent(UUID.randomUUID(), Instant.now());

        aggregate.addEvent(event);

        assertThat(aggregate.getDomainEvents()).containsExactly(event);
    }

    @Test
    void registerMultipleEvents_shouldPreserveOrder() {
        var aggregate = new TestAggregate(UUID.randomUUID());
        var eventA = new TestEvent(UUID.randomUUID(), Instant.now());
        var eventB = new TestEvent(UUID.randomUUID(), Instant.now());
        var eventC = new TestEvent(UUID.randomUUID(), Instant.now());

        aggregate.addEvent(eventA);
        aggregate.addEvent(eventB);
        aggregate.addEvent(eventC);

        assertThat(aggregate.getDomainEvents()).containsExactly(eventA, eventB, eventC);
    }

    @Test
    void getDomainEvents_shouldReturnDefensiveCopy() {
        var aggregate = new TestAggregate(UUID.randomUUID());
        var event = new TestEvent(UUID.randomUUID(), Instant.now());
        aggregate.addEvent(event);

        List<DomainEvent> events = aggregate.getDomainEvents();

        assertThatThrownBy(() -> events.add(new TestEvent(UUID.randomUUID(), Instant.now())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void clearDomainEvents_shouldReturnEmptyList() {
        var aggregate = new TestAggregate(UUID.randomUUID());
        aggregate.addEvent(new TestEvent(UUID.randomUUID(), Instant.now()));
        aggregate.addEvent(new TestEvent(UUID.randomUUID(), Instant.now()));

        aggregate.clearDomainEvents();

        assertThat(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    void newAggregate_shouldHaveNoDomainEvents() {
        var aggregate = new TestAggregate(UUID.randomUUID());

        assertThat(aggregate.getDomainEvents()).isEmpty();
    }
}
