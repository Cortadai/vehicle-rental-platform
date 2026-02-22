package com.vehiclerental.payment.infrastructure.adapter.output.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.PaymentStatus;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import com.vehiclerental.payment.infrastructure.adapter.output.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.List;

@Component
public class PaymentPersistenceMapper {

    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public PaymentPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PaymentJpaEntity toJpaEntity(Payment payment) {
        var entity = new PaymentJpaEntity();
        entity.setId(payment.getId().value());
        entity.setReservationId(payment.getReservationId().value());
        entity.setCustomerId(payment.getCustomerId().value());
        entity.setAmount(payment.getAmount().amount());
        entity.setCurrency(payment.getAmount().currency().getCurrencyCode());
        entity.setStatus(payment.getStatus().name());
        entity.setFailureMessages(serializeFailureMessages(payment.getFailureMessages()));
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());
        return entity;
    }

    public Payment toDomainEntity(PaymentJpaEntity entity) {
        return Payment.reconstruct(
                new PaymentId(entity.getId()),
                new ReservationId(entity.getReservationId()),
                new CustomerId(entity.getCustomerId()),
                new Money(entity.getAmount(), Currency.getInstance(entity.getCurrency())),
                PaymentStatus.valueOf(entity.getStatus()),
                deserializeFailureMessages(entity.getFailureMessages()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String serializeFailureMessages(List<String> failureMessages) {
        if (failureMessages == null || failureMessages.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(failureMessages);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize failureMessages", e);
        }
    }

    private List<String> deserializeFailureMessages(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_OF_STRINGS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize failureMessages", e);
        }
    }
}
