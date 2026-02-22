package com.vehiclerental.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.application.mapper.ReservationApplicationMapper;
import com.vehiclerental.reservation.application.port.input.CreateReservationUseCase;
import com.vehiclerental.reservation.application.port.input.TrackReservationUseCase;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import com.vehiclerental.reservation.application.service.ReservationApplicationService;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.mapper.ReservationPersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ReservationApplicationService reservationApplicationService(
            ReservationRepository reservationRepository,
            ReservationDomainEventPublisher eventPublisher,
            ReservationApplicationMapper mapper) {
        return new ReservationApplicationService(reservationRepository, eventPublisher, mapper);
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
