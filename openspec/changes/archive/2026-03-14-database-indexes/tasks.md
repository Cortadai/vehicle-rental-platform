## 1. Flyway Migrations

- [x] 1.1 Create `reservation-service/reservation-container/src/main/resources/db/migration/V4__create_indexes.sql` with indexes on `reservations(customer_id)` and `reservations(status)`
- [x] 1.2 Create `payment-service/payment-container/src/main/resources/db/migration/V3__create_indexes.sql` with index on `payments(status)`
- [x] 1.3 Create `fleet-service/fleet-container/src/main/resources/db/migration/V3__create_indexes.sql` with index on `vehicles(status)`

## 2. Verification

- [x] 2.1 Run `mvn verify` — all tests pass including Flyway migrations with Testcontainers
