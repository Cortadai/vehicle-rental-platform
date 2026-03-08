package com.vehiclerental.reservation.application.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import com.vehiclerental.reservation.domain.port.output.SagaStateRepository;
import com.vehiclerental.reservation.domain.model.saga.SagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class ReservationSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReservationSagaOrchestrator.class);
    private static final String SAGA_TYPE = "RESERVATION_CREATION";

    private final List<SagaStep<ReservationSagaData>> steps;
    private final SagaStateRepository sagaStateRepository;
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;

    public ReservationSagaOrchestrator(List<SagaStep<ReservationSagaData>> steps,
                                       SagaStateRepository sagaStateRepository,
                                       ReservationRepository reservationRepository,
                                       ObjectMapper objectMapper) {
        this.steps = steps;
        this.sagaStateRepository = sagaStateRepository;
        this.reservationRepository = reservationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void start(ReservationSagaData sagaData) {
        log.info("Starting SAGA for reservation {}", sagaData.reservationId());
        String payload = serialize(sagaData);
        SagaState sagaState = SagaState.create(sagaData.reservationId(), SAGA_TYPE, steps.size(), payload);
        sagaState.beginProcessing();
        sagaStateRepository.save(sagaState);
        steps.get(0).process(sagaData);
    }

    @Transactional
    public void handleStepSuccess(UUID reservationId, String stepName) {
        log.info("Step {} succeeded for reservation {}", stepName, reservationId);
        SagaState sagaState = loadSagaState(reservationId);
        Reservation reservation = loadReservation(reservationId);
        ReservationSagaData sagaData = deserialize(sagaState.getPayload());

        transitionReservationOnSuccess(reservation, stepName);

        int nextStepIndex = sagaState.getCurrentStep() + 1;
        if (nextStepIndex < steps.size()) {
            sagaState.advanceToNextStep();
            sagaStateRepository.save(sagaState);
            reservationRepository.save(reservation);
            steps.get(nextStepIndex).process(sagaData);
        } else {
            sagaState.markAsSucceeded();
            sagaStateRepository.save(sagaState);
            reservationRepository.save(reservation);
            log.info("SAGA completed successfully for reservation {}", reservationId);
        }
    }

    @Transactional
    public void handleStepFailure(UUID reservationId, String stepName, List<String> failureMessages) {
        log.warn("Step {} failed for reservation {}: {}", stepName, reservationId, failureMessages);
        SagaState sagaState = loadSagaState(reservationId);
        Reservation reservation = loadReservation(reservationId);
        ReservationSagaData sagaData = deserialize(sagaState.getPayload());

        String reason = String.join("; ", failureMessages);
        sagaState.startCompensation(reason);

        int failedStepIndex = getStepIndex(stepName);
        int compensateIndex = findNextCompensatableStep(failedStepIndex - 1);

        if (compensateIndex >= 0) {
            reservation.initCancel(failureMessages);
            steps.get(compensateIndex).rollback(sagaData);
            log.info("Starting compensation at step {} for reservation {}", steps.get(compensateIndex).getName(), reservationId);
        } else {
            reservation.cancel();
            sagaState.markAsFailed();
            log.info("No compensations needed, SAGA failed for reservation {}", reservationId);
        }

        sagaStateRepository.save(sagaState);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void handleCompensationComplete(UUID reservationId, String stepName) {
        log.info("Compensation for step {} completed for reservation {}", stepName, reservationId);
        SagaState sagaState = loadSagaState(reservationId);
        Reservation reservation = loadReservation(reservationId);
        ReservationSagaData sagaData = deserialize(sagaState.getPayload());

        int completedStepIndex = getStepIndex(stepName);
        int nextCompensateIndex = findNextCompensatableStep(completedStepIndex - 1);

        if (nextCompensateIndex >= 0) {
            steps.get(nextCompensateIndex).rollback(sagaData);
            log.info("Continuing compensation at step {} for reservation {}", steps.get(nextCompensateIndex).getName(), reservationId);
        } else {
            reservation.cancel();
            sagaState.markAsFailed();
            log.info("All compensations complete, SAGA failed for reservation {}", reservationId);
        }

        sagaStateRepository.save(sagaState);
        reservationRepository.save(reservation);
    }

    private void transitionReservationOnSuccess(Reservation reservation, String stepName) {
        switch (stepName) {
            case "CUSTOMER_VALIDATION" -> reservation.validateCustomer();
            case "PAYMENT" -> reservation.pay();
            case "FLEET_CONFIRMATION" -> reservation.confirm();
            default -> throw new IllegalArgumentException("Unknown step name: " + stepName);
        }
    }

    private int getStepIndex(String stepName) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getName().equals(stepName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown step: " + stepName);
    }

    private int findNextCompensatableStep(int fromIndex) {
        for (int i = fromIndex; i >= 0; i--) {
            if (steps.get(i).hasCompensation()) {
                return i;
            }
        }
        return -1;
    }

    private SagaState loadSagaState(UUID reservationId) {
        return sagaStateRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("SagaState not found for reservation " + reservationId));
    }

    private Reservation loadReservation(UUID reservationId) {
        return reservationRepository.findById(new ReservationId(reservationId))
                .orElseThrow(() -> new IllegalStateException("Reservation not found: " + reservationId));
    }

    private String serialize(ReservationSagaData sagaData) {
        try {
            return objectMapper.writeValueAsString(sagaData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ReservationSagaData", e);
        }
    }

    private ReservationSagaData deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, ReservationSagaData.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize ReservationSagaData", e);
        }
    }
}
