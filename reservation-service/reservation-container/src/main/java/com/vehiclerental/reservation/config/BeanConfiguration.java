package com.vehiclerental.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.application.mapper.ReservationApplicationMapper;
import com.vehiclerental.reservation.application.port.input.CreateReservationUseCase;
import com.vehiclerental.reservation.application.port.input.TrackReservationUseCase;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import com.vehiclerental.reservation.application.port.output.SagaCommandPublisher;
import com.vehiclerental.reservation.application.saga.CustomerValidationStep;
import com.vehiclerental.reservation.application.saga.FleetConfirmationStep;
import com.vehiclerental.reservation.application.saga.PaymentStep;
import com.vehiclerental.reservation.application.saga.ReservationSagaData;
import com.vehiclerental.reservation.application.saga.ReservationSagaOrchestrator;
import com.vehiclerental.reservation.application.saga.SagaStep;
import com.vehiclerental.reservation.application.service.ReservationApplicationService;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import com.vehiclerental.reservation.domain.port.output.SagaStateRepository;
import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.mapper.ReservationPersistenceMapper;
import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.saga.SagaStatePersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BeanConfiguration {

    @Bean
    public ReservationPersistenceMapper reservationPersistenceMapper(ObjectMapper objectMapper) {
        return new ReservationPersistenceMapper(objectMapper);
    }

    @Bean
    public ReservationApplicationMapper reservationApplicationMapper() {
        return new ReservationApplicationMapper();
    }

    @Bean
    public SagaStatePersistenceMapper sagaStatePersistenceMapper() {
        return new SagaStatePersistenceMapper();
    }

    @Bean
    public CustomerValidationStep customerValidationStep(SagaCommandPublisher sagaCommandPublisher,
                                                         ObjectMapper objectMapper) {
        return new CustomerValidationStep(sagaCommandPublisher, objectMapper);
    }

    @Bean
    public PaymentStep paymentStep(SagaCommandPublisher sagaCommandPublisher,
                                   ObjectMapper objectMapper) {
        return new PaymentStep(sagaCommandPublisher, objectMapper);
    }

    @Bean
    public FleetConfirmationStep fleetConfirmationStep(SagaCommandPublisher sagaCommandPublisher,
                                                       ObjectMapper objectMapper) {
        return new FleetConfirmationStep(sagaCommandPublisher, objectMapper);
    }

    @Bean
    public ReservationSagaOrchestrator reservationSagaOrchestrator(
            CustomerValidationStep customerValidationStep,
            PaymentStep paymentStep,
            FleetConfirmationStep fleetConfirmationStep,
            SagaStateRepository sagaStateRepository,
            ReservationRepository reservationRepository,
            ObjectMapper objectMapper) {
        List<SagaStep<ReservationSagaData>> steps = List.of(
                customerValidationStep, paymentStep, fleetConfirmationStep);
        return new ReservationSagaOrchestrator(steps, sagaStateRepository, reservationRepository, objectMapper);
    }

    @Bean
    public ReservationApplicationService reservationApplicationService(
            ReservationRepository reservationRepository,
            ReservationDomainEventPublisher eventPublisher,
            ReservationApplicationMapper mapper,
            ReservationSagaOrchestrator sagaOrchestrator) {
        return new ReservationApplicationService(reservationRepository, eventPublisher, mapper, sagaOrchestrator);
    }

    @Bean
    public CreateReservationUseCase createReservationUseCase(ReservationApplicationService service) {
        return service;
    }

    @Bean
    public TrackReservationUseCase trackReservationUseCase(ReservationApplicationService service) {
        return service;
    }
}
