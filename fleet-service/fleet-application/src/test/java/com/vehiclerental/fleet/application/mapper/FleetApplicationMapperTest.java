package com.vehiclerental.fleet.application.mapper;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;
import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FleetApplicationMapperTest {

    private final FleetApplicationMapper mapper = new FleetApplicationMapper();

    @Test
    void toResponseMapsAllFieldsCorrectly() {
        var vehicleId = new VehicleId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        var licensePlate = new LicensePlate("ABC-123");
        var dailyRate = new DailyRate(new Money(new BigDecimal("45.00"), Currency.getInstance("USD")));
        var createdAt = Instant.parse("2024-01-15T10:30:00Z");

        var vehicle = Vehicle.reconstruct(
                vehicleId, licensePlate, "Toyota", "Corolla", 2024,
                VehicleCategory.SEDAN, dailyRate, "Compact sedan",
                VehicleStatus.ACTIVE, createdAt);

        VehicleResponse response = mapper.toResponse(vehicle);

        assertThat(response.vehicleId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.licensePlate()).isEqualTo("ABC-123");
        assertThat(response.make()).isEqualTo("Toyota");
        assertThat(response.model()).isEqualTo("Corolla");
        assertThat(response.year()).isEqualTo(2024);
        assertThat(response.category()).isEqualTo("SEDAN");
        assertThat(response.dailyRateAmount()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(response.dailyRateCurrency()).isEqualTo("USD");
        assertThat(response.description()).isEqualTo("Compact sedan");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponseMapsNullDescriptionAsNull() {
        var vehicleId = new VehicleId(UUID.randomUUID());
        var licensePlate = new LicensePlate("XY-99");
        var dailyRate = new DailyRate(new Money(new BigDecimal("30.00"), Currency.getInstance("EUR")));
        var createdAt = Instant.now();

        var vehicle = Vehicle.reconstruct(
                vehicleId, licensePlate, "Honda", "Civic", 2023,
                VehicleCategory.SEDAN, dailyRate, null,
                VehicleStatus.ACTIVE, createdAt);

        VehicleResponse response = mapper.toResponse(vehicle);

        assertThat(response.description()).isNull();
    }
}
