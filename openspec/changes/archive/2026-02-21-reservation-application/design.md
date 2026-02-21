## Context

The reservation-domain module is complete with: Reservation aggregate root (6 states: PENDING, CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, CANCELLED; 6 transition methods), ReservationItem inner entity (subtotal = dailyRate x days), typed IDs (ReservationId, TrackingId, CustomerId, VehicleId — all local to the bounded context), DateRange (pickupDate + returnDate with getDays()), PickupLocation (address + city), Money from common (totalPrice, dailyRate, subtotal), ReservationDomainException with error codes, and ReservationRepository output port (save, findById, findByTrackingId).

Customer-application (17 tests, 13 classes) and fleet-application established the application layer pattern: one input port per use case (ISP), command/response records with primitives, a DomainEventPublisher output port, a single Application Service implementing all input ports with `@Transactional`, manual mapper, and NotFoundException extending RuntimeException in the application module.

The application layer bridges the domain with the outside world. Per docs/17, it's a separate Maven module (`reservation-application`) that depends on `reservation-domain` and adds only `spring-tx` for `@Transactional`. Infrastructure adapters (future change) will depend on this module to call use cases through input ports.

Reservation's application layer is more complex than Customer/Fleet because: (1) creation involves ReservationItems with compound types (VehicleId + Money + days), (2) there are two lookup mechanisms (by internal ReservationId vs. public TrackingId), and (3) the response for tracking includes nested item details. Only 2 use cases are in scope (create + track) — the SAGA transition use cases (validateCustomer, pay, confirm, initCancel, cancel) will be added when the SAGA orchestrator is implemented.

## Goals / Non-Goals

**Goals:**

* Replicate the application layer pattern established by customer-application and fleet-application
* Define input ports: CreateReservationUseCase, TrackReservationUseCase (one interface per use case)
* Define output port: ReservationDomainEventPublisher for domain event dispatch contract
* Add command records with primitives: CreateReservationCommand (with inner CreateReservationItemCommand), TrackReservationCommand
* Add response records: CreateReservationResponse (lean: trackingId + status), TrackReservationResponse (full snapshot with nested items)
* Implement ReservationApplicationService as pure orchestrator (zero business logic) with save → publish → clearDomainEvents cycle
* Add ReservationApplicationMapper (manual, plain Java) for domain ↔ DTO translation
* Add ReservationNotFoundException for tracking ID not found scenarios (application-level exception)
* Unit test the application service with mocked output ports, including annotation verification
* Update root POM to include reservation-application module

**Non-Goals:**

* SAGA transition use cases (validateCustomer, pay, confirm, initCancel, cancel) — added when SAGA orchestrator is built
* Implementation of EventPublisher (Outbox, RabbitMQ) — belongs in infrastructure; only the port contract is defined here
* Domain Service — no cross-aggregate coordination exists in this phase
* REST controller, JPA entities, Flyway migrations — change `reservation-infrastructure`
* BeanConfiguration — change `reservation-container` (container module)
* MapStruct mapper — manual mapper is sufficient for the current field count
* Input validation on commands — validation happens in domain Value Objects (CustomerId, PickupLocation, DateRange constructors)
* Validation `pickupDate >= today` — application-level rule deferred to a future use case; keeps scope minimal
* Relocating ReservationRepository — the output port stays in domain where it was placed in reservation-domain change

## Decisions

### Decision 1: ReservationRepository stays in domain

**Choice**: Leave `ReservationRepository` in `reservation-domain/port/output/`. The application module depends on reservation-domain and uses the port directly.

**Rationale**: Same decision as customer-application and fleet-application. The repository interface uses only domain types (`Reservation`, `ReservationId`, `TrackingId`). Moving it would add churn for no functional benefit. The interface is framework-free regardless of location.

**Alternative rejected**: Move to `reservation-application/port/output/`. Unnecessary — the interface compiles and lives cleanly in domain.

### Decision 2: One Application Service implementing both input ports

**Choice**: A single `ReservationApplicationService` implements `CreateReservationUseCase` and `TrackReservationUseCase`.

**Rationale**: Two use cases with straightforward orchestration don't justify two separate service classes. Each use case has its own interface (ISP), so extraction is trivial if complexity grows (e.g., when SAGA use cases are added, they could go in a separate service class).

**Alternative rejected**: One service class per use case. Over-engineering for 2 simple orchestration methods.

### Decision 3: CreateReservationCommand carries primitives, with inner record for items

**Choice**: `CreateReservationCommand` carries `String customerId`, `String pickupAddress`, `String pickupCity`, `String returnAddress`, `String returnCity`, `String pickupDate`, `String returnDate`, `String currency`, and `List<CreateReservationItemCommand> items`. The inner `CreateReservationItemCommand` carries `String vehicleId`, `BigDecimal dailyRate`, `int days`.

**Rationale**: Commands are the boundary between the outside world (REST controllers, message listeners) and the domain. The outside world speaks primitives. Converting to typed IDs, PickupLocation, DateRange, Money, and ReservationItem is the application layer's responsibility. Dates as ISO strings (not LocalDate) because the command is framework-agnostic — parsing happens in the application service. Currency at the command level (not per-item) because all items in a reservation share the same currency — this is a simplification for the POC that maps naturally to REST API design.

**Alternative rejected**: Commands with domain types (CustomerId, PickupLocation, DateRange). Would leak domain types into infrastructure.

### Decision 4: TrackReservationCommand as a record, not raw String

**Choice**: `TrackReservationCommand(String trackingId)` record, not `execute(String trackingId)`.

**Rationale**: Customer uses `GetCustomerCommand(String customerId)`, Fleet uses `GetVehicleCommand(String vehicleId)`. A record with a single field may seem ceremonious, but pattern consistency matters across the platform. If the use case grows (e.g., adding include-items flag, locale for formatting), the command structure is already in place.

**Alternative rejected**: Raw String parameter. Breaks the established command-record convention.

### Decision 5: Two separate response types — lean create, full track

**Choice**: `CreateReservationResponse(String trackingId, String status)` for creation. `TrackReservationResponse(String trackingId, String customerId, ..., List<TrackReservationItemResponse> items, List<String> failureMessages, Instant createdAt)` for tracking.

**Rationale**: CQS-inspired separation. The create response only returns what the client needs to track the reservation — the trackingId and initial status (PENDING). The client already has the details they sent. The track response is a full snapshot for the "query" side. Having a single response type would either bloat the create response or starve the track response.

**Alternative rejected**: Single `ReservationResponse` for both. Would couple query and command concerns.

### Decision 6: Lifecycle use case (create) returns response, query (track) returns response

**Choice**: `CreateReservationUseCase` returns `CreateReservationResponse`. `TrackReservationUseCase` returns `TrackReservationResponse`.

**Rationale**: Unlike Customer/Fleet where lifecycle methods (suspend, activate, delete) return void (CQS), Reservation's create needs to return the trackingId — the client doesn't know it before creation. This is the same pattern as Customer's create (returns CustomerResponse) and Fleet's register (returns VehicleResponse). The track use case is a pure query, so returning the full response is natural.

### Decision 7: ReservationDomainEventPublisher output port — contract only

**Choice**: `ReservationDomainEventPublisher` interface with `void publish(List<DomainEvent> domainEvents)`, defined in `application/port/output/`. The application service calls `publish(reservation.getDomainEvents())` then `reservation.clearDomainEvents()`.

**Rationale**: Customer-application and fleet-application both define their EventPublisher port and call the full cycle (save → publish → clearDomainEvents). Omitting the port would break the established pattern and leave domain events accumulated in the aggregate without dispatch or cleanup. The implementation (Outbox table, direct RabbitMQ, in-memory for tests) belongs in infrastructure — the port is just the contract.

**Alternative rejected**: Skip EventPublisher until Phase 2. Would deviate from the pattern and require retroactive changes to the application service when Outbox is added.

### Decision 8: ReservationNotFoundException extends RuntimeException

**Choice**: `ReservationNotFoundException` extends `RuntimeException` directly, NOT `ReservationDomainException`. Lives in the application module under `exception/`. Carries the tracking ID as a String for diagnostic purposes.

**Rationale**: Same reasoning as customer-application and fleet-application. "Not found" is an application-level concern — the domain doesn't know about persistence queries. Domain exceptions represent invariant violations (invalid state transitions, validation failures). Keeping them separate lets the GlobalExceptionHandler distinguish: `ReservationDomainException → 422`, `ReservationNotFoundException → 404`.

**Alternative rejected**: Extend `ReservationDomainException`. Conflates invariant violations with lookup failures.

### Decision 9: Manual mapper, no MapStruct

**Choice**: `ReservationApplicationMapper` is a plain Java class with methods for command-to-domain and domain-to-response conversions.

**Rationale**: Same reasoning as customer-application and fleet-application. The mapper handles ~15 fields across two response types plus item conversion. MapStruct adds annotation processor complexity for marginal benefit. Manual mapping is explicit and debuggable.

**Alternative rejected**: MapStruct. Overhead exceeds benefit for this field count.

### Decision 10: Test files split by use case

**Choice**: `ReservationApplicationServiceCreateTest` and `ReservationApplicationServiceTrackTest` as separate test classes, plus `ReservationNotFoundExceptionTest` and `ReservationApplicationMapperTest`.

**Rationale**: The create flow has more test scenarios (happy path, items mapping, currency conversion, event publishing, event clearing) than a single test file would comfortably hold. Splitting by use case improves readability and makes test failures easier to locate. Customer and Fleet use a single test file because they have fewer scenarios.

**Alternative rejected**: Single `ReservationApplicationServiceTest`. Would become unwieldy with 10+ test methods covering two distinct flows.

## Package Structure

```
reservation-service/reservation-application/src/main/java/com/vehiclerental/reservation/application/
├── port/
│   ├── input/
│   │   ├── CreateReservationUseCase.java
│   │   └── TrackReservationUseCase.java
│   └── output/
│       └── ReservationDomainEventPublisher.java
├── dto/
│   ├── command/
│   │   ├── CreateReservationCommand.java (+ inner CreateReservationItemCommand)
│   │   └── TrackReservationCommand.java
│   └── response/
│       ├── CreateReservationResponse.java
│       └── TrackReservationResponse.java (+ inner TrackReservationItemResponse)
├── service/
│   └── ReservationApplicationService.java
├── mapper/
│   └── ReservationApplicationMapper.java
└── exception/
    └── ReservationNotFoundException.java
```

## Risks / Trade-offs

* **spring-tx dependency in application** — `@Transactional` couples the application layer to Spring. Mitigation: it's a single annotation import with no runtime dependency on Spring Context. The alternative (TransactionPort interface) adds complexity for zero practical benefit. Consistent with customer-application and fleet-application.
* **Single Application Service may not scale** — When SAGA use cases are added (5 more transitions), the single class could grow to 7 methods. Mitigation: each use case has its own interface (ISP), so extraction into `ReservationSagaStepService` is a mechanical refactor. The SAGA use cases have a different lifecycle (triggered by messages, not REST) which naturally suggests a separate class.
* **Currency at command level, not per-item** — Assumes all items in a reservation share the same currency. If multi-currency reservations are needed, the command structure must change. Mitigation: this is a POC decision; Money in common already supports currency, so the domain can handle it. The application layer command is the only thing that would need restructuring.
* **ReservationNotFoundException as standalone RuntimeException** — If more application exceptions appear (e.g., `ReservationAlreadyCancelledException`), a common `ReservationApplicationException` base could be introduced. One exception doesn't justify a hierarchy. Same trade-off as customer-application.
* **Only 2 use cases now, 7+ later** — The application layer will grow when SAGA transitions are added. Risk: retroactive changes to tests and service. Mitigation: the EventPublisher port and the save-publish-clear cycle are already in place, so SAGA use cases will follow the same orchestration pattern with no structural changes.
