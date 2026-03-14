## 1. Maven Dependencies

- [x] 1.1 Add `springdoc-openapi-starter-webmvc-ui` to `customer-service/customer-container/pom.xml`
- [x] 1.2 Add `springdoc-openapi-starter-webmvc-ui` to `fleet-service/fleet-container/pom.xml`
- [x] 1.3 Add `springdoc-openapi-starter-webmvc-ui` to `reservation-service/reservation-container/pom.xml`
- [x] 1.4 Add `springdoc-openapi-starter-webmvc-ui` to `payment-service/payment-container/pom.xml`

## 2. Controller Annotations

- [x] 2.1 Annotate `CustomerController` with @Tag, @Operation, @ApiResponse on all 5 methods
- [x] 2.2 Annotate `VehicleController` with @Tag, @Operation, @ApiResponse on all 5 methods
- [x] 2.3 Annotate `ReservationController` with @Tag, @Operation, @ApiResponse on all 2 methods
- [x] 2.4 Annotate `PaymentController` with @Tag, @Operation, @ApiResponse on all 3 methods

## 3. Verification

- [x] 3.1 Run `mvn test` — all tests pass (ArchUnit not triggered by swagger annotations)
- [x] 3.2 Update CLAUDE.md with Swagger UI URLs
