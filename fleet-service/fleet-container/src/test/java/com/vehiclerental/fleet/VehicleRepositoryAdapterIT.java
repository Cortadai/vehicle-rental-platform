package com.vehiclerental.fleet;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.DailyRate;
import com.vehiclerental.fleet.domain.model.vo.LicensePlate;
import com.vehiclerental.fleet.domain.model.vo.VehicleCategory;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;
import com.vehiclerental.fleet.domain.port.output.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VehicleRepositoryAdapterIT {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void saveAndFindByIdRoundTrip() {
        var vehicle = Vehicle.create(
                new LicensePlate("AB-123-CD"),
                "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN,
                new DailyRate(new Money(new BigDecimal("49.99"), Currency.getInstance("USD"))),
                "Comfortable sedan");
        vehicle.clearDomainEvents();

        var saved = vehicleRepository.save(vehicle);

        var found = vehicleRepository.findById(saved.getId());

        assertThat(found).isPresent();
        var loaded = found.get();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getLicensePlate().value()).isEqualTo("AB-123-CD");
        assertThat(loaded.getMake()).isEqualTo("Toyota");
        assertThat(loaded.getModel()).isEqualTo("Corolla");
        assertThat(loaded.getYear()).isEqualTo(2023);
        assertThat(loaded.getCategory()).isEqualTo(VehicleCategory.SEDAN);
        assertThat(loaded.getDailyRate().money().amount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(loaded.getDailyRate().money().currency().getCurrencyCode()).isEqualTo("USD");
        assertThat(loaded.getDescription()).isEqualTo("Comfortable sedan");
        assertThat(loaded.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(loaded.getCreatedAt()).isCloseTo(saved.getCreatedAt(),
                within(1, java.time.temporal.ChronoUnit.MICROS));
    }

    @Test
    void findByIdReturnsEmptyForNonExisting() {
        var nonExistingId = new VehicleId(UUID.randomUUID());

        var result = vehicleRepository.findById(nonExistingId);

        assertThat(result).isEmpty();
    }

    @Test
    void savePersistsAllFieldsIncludingNullDescription() {
        var vehicle = Vehicle.create(
                new LicensePlate("XY-789-ZZ"),
                "Honda", "Civic", 2024,
                VehicleCategory.SEDAN,
                new DailyRate(new Money(new BigDecimal("35.00"), Currency.getInstance("EUR"))),
                null);
        vehicle.clearDomainEvents();

        var saved = vehicleRepository.save(vehicle);
        var found = vehicleRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isNull();
        assertThat(found.get().getDailyRate().money().amount()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(found.get().getDailyRate().money().currency().getCurrencyCode()).isEqualTo("EUR");
    }
}
