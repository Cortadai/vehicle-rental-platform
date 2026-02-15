package com.vehiclerental.fleet.application.service;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.application.dto.command.*;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;
import com.vehiclerental.fleet.application.exception.VehicleNotFoundException;
import com.vehiclerental.fleet.application.mapper.FleetApplicationMapper;
import com.vehiclerental.fleet.application.port.output.FleetDomainEventPublisher;
import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.*;
import com.vehiclerental.fleet.domain.port.output.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FleetApplicationServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private FleetDomainEventPublisher eventPublisher;

    private final FleetApplicationMapper mapper = new FleetApplicationMapper();

    private FleetApplicationService service;

    private static final UUID VEHICLE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String VEHICLE_ID_STR = VEHICLE_UUID.toString();

    @BeforeEach
    void setUp() {
        service = new FleetApplicationService(vehicleRepository, eventPublisher, mapper);
    }

    @Nested
    class RegisterVehicle {

        @Test
        void savesVehicleAndPublishesEventsAndClearsEventsAndReturnsResponse() {
            var command = new RegisterVehicleCommand(
                    "ABC-123", "Toyota", "Corolla", 2024,
                    "SEDAN", new BigDecimal("45.00"), "USD", "Compact sedan");
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            VehicleResponse response = service.execute(command);

            var vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
            InOrder inOrder = inOrder(vehicleRepository, eventPublisher);
            inOrder.verify(vehicleRepository).save(vehicleCaptor.capture());
            inOrder.verify(eventPublisher).publish(any());

            Vehicle savedVehicle = vehicleCaptor.getValue();
            assertThat(savedVehicle.getLicensePlate().value()).isEqualTo("ABC-123");
            assertThat(savedVehicle.getMake()).isEqualTo("Toyota");
            assertThat(savedVehicle.getModel()).isEqualTo("Corolla");
            assertThat(savedVehicle.getCategory()).isEqualTo(VehicleCategory.SEDAN);

            assertThat(response).isNotNull();
            assertThat(response.licensePlate()).isEqualTo("ABC-123");
            assertThat(response.make()).isEqualTo("Toyota");
            assertThat(response.model()).isEqualTo("Corolla");
        }
    }

    @Nested
    class GetVehicle {

        @Test
        void returnsResponseWhenVehicleFound() {
            var vehicle = buildActiveVehicle();
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.of(vehicle));

            var command = new GetVehicleCommand(VEHICLE_ID_STR);
            VehicleResponse response = service.execute(command);

            assertThat(response.vehicleId()).isEqualTo(VEHICLE_ID_STR);
            assertThat(response.licensePlate()).isEqualTo("ABC-123");
        }

        @Test
        void throwsVehicleNotFoundExceptionWhenNotFound() {
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.empty());

            var command = new GetVehicleCommand(VEHICLE_ID_STR);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(VehicleNotFoundException.class)
                    .hasMessageContaining(VEHICLE_ID_STR);
        }
    }

    @Nested
    class SendToMaintenance {

        @Test
        void loadsAndSendsToMaintenanceAndSavesAndPublishesEventsAndClearsEvents() {
            var vehicle = buildActiveVehicle();
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new SendToMaintenanceCommand(VEHICLE_ID_STR));

            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.UNDER_MAINTENANCE);

            InOrder inOrder = inOrder(vehicleRepository, eventPublisher);
            inOrder.verify(vehicleRepository).findById(any());
            inOrder.verify(vehicleRepository).save(vehicle);
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void throwsVehicleNotFoundExceptionWhenNotFound() {
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new SendToMaintenanceCommand(VEHICLE_ID_STR)))
                    .isInstanceOf(VehicleNotFoundException.class);
        }
    }

    @Nested
    class ActivateVehicle {

        @Test
        void loadsAndActivatesAndSavesAndPublishesEventsAndClearsEvents() {
            var vehicle = buildUnderMaintenanceVehicle();
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new ActivateVehicleCommand(VEHICLE_ID_STR));

            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);

            InOrder inOrder = inOrder(vehicleRepository, eventPublisher);
            inOrder.verify(vehicleRepository).findById(any());
            inOrder.verify(vehicleRepository).save(vehicle);
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void throwsVehicleNotFoundExceptionWhenNotFound() {
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new ActivateVehicleCommand(VEHICLE_ID_STR)))
                    .isInstanceOf(VehicleNotFoundException.class);
        }
    }

    @Nested
    class RetireVehicle {

        @Test
        void loadsAndRetiresAndSavesAndPublishesEventsAndClearsEvents() {
            var vehicle = buildActiveVehicle();
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(new RetireVehicleCommand(VEHICLE_ID_STR));

            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RETIRED);

            InOrder inOrder = inOrder(vehicleRepository, eventPublisher);
            inOrder.verify(vehicleRepository).findById(any());
            inOrder.verify(vehicleRepository).save(vehicle);
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void throwsVehicleNotFoundExceptionWhenNotFound() {
            when(vehicleRepository.findById(new VehicleId(VEHICLE_UUID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new RetireVehicleCommand(VEHICLE_ID_STR)))
                    .isInstanceOf(VehicleNotFoundException.class);
        }
    }

    @Nested
    class AnnotationChecks {

        @Test
        void classHasNoServiceOrComponentAnnotation() {
            assertThat(FleetApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(FleetApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }

        @Test
        void writeMethodsAreTransactional() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", RegisterVehicleCommand.class, false);
            assertMethodIsTransactional("execute", SendToMaintenanceCommand.class, false);
            assertMethodIsTransactional("execute", ActivateVehicleCommand.class, false);
            assertMethodIsTransactional("execute", RetireVehicleCommand.class, false);
        }

        @Test
        void getMethodIsReadOnlyTransactional() throws NoSuchMethodException {
            assertMethodIsTransactional("execute", GetVehicleCommand.class, true);
        }

        private void assertMethodIsTransactional(String methodName, Class<?> paramType, boolean readOnly)
                throws NoSuchMethodException {
            Method method = FleetApplicationService.class.getMethod(methodName, paramType);
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isEqualTo(readOnly);
        }
    }

    private Vehicle buildActiveVehicle() {
        return Vehicle.reconstruct(
                new VehicleId(VEHICLE_UUID),
                new LicensePlate("ABC-123"),
                "Toyota", "Corolla", 2024,
                VehicleCategory.SEDAN,
                new DailyRate(new Money(new BigDecimal("45.00"), Currency.getInstance("USD"))),
                "Compact sedan",
                VehicleStatus.ACTIVE,
                Instant.parse("2024-01-15T10:30:00Z"));
    }

    private Vehicle buildUnderMaintenanceVehicle() {
        return Vehicle.reconstruct(
                new VehicleId(VEHICLE_UUID),
                new LicensePlate("ABC-123"),
                "Toyota", "Corolla", 2024,
                VehicleCategory.SEDAN,
                new DailyRate(new Money(new BigDecimal("45.00"), Currency.getInstance("USD"))),
                "Compact sedan",
                VehicleStatus.UNDER_MAINTENANCE,
                Instant.parse("2024-01-15T10:30:00Z"));
    }
}
