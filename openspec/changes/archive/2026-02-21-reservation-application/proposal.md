## Why

The reservation-domain module is complete (6-state aggregate, ReservationItem inner entity, typed IDs, DateRange, PickupLocation, ReservationRepository). The domain has no way to be invoked — there are no use cases, commands, or DTOs. The application layer is needed to bridge the domain with the outside world: receive primitive inputs, convert to domain objects, orchestrate the aggregate, persist, publish events, and return responses. This is the second of three changes for Reservation Service (domain -> **application** -> infrastructure+container).

## What Changes

- Add `reservation-application` Maven module with `reservation-domain` + `spring-tx` dependencies
- Define input ports: `CreateReservationUseCase`, `TrackReservationUseCase` (one interface per use case, ISP)
- Define output port: `ReservationDomainEventPublisher` for domain event dispatch contract
- Add command records: `CreateReservationCommand` (with inner `CreateReservationItemCommand`), `TrackReservationCommand`
- Add response records: `CreateReservationResponse` (lean: trackingId + status), `TrackReservationResponse` (full snapshot with items)
- Implement `ReservationApplicationService` — pure orchestrator with save -> publish -> clearDomainEvents cycle
- Add `ReservationApplicationMapper` — manual domain-to-DTO mapping (no MapStruct)
- Add `ReservationNotFoundException` — application-level exception for tracking ID not found (-> 404)
- Update root POM: add module declaration and dependencyManagement entry
- Unit tests with Mockito verifying orchestration, annotations, and event lifecycle

## Capabilities

### New Capabilities

- `reservation-application-ports`: Input ports (CreateReservationUseCase, TrackReservationUseCase) and output port (ReservationDomainEventPublisher) defining the application boundary
- `reservation-application-dtos`: Command records (CreateReservationCommand with inner item command, TrackReservationCommand) and response records (CreateReservationResponse, TrackReservationResponse with inner item response)
- `reservation-application-service`: ReservationApplicationService implementing both input ports with @Transactional, orchestrating create (command -> domain -> save -> publish -> clearDomainEvents -> response) and track (command -> repository lookup -> not-found check -> response) flows
- `multi-module-build`: Root POM updated with reservation-application module declaration and dependencyManagement entry

### Modified Capabilities

_(none — no existing spec requirements change)_

## Impact

- **New module**: `reservation-service/reservation-application/` with its own `pom.xml`
- **Root POM**: New module in `<modules>` and `<dependencyManagement>` (after `reservation-domain`, before `reservation-infrastructure`)
- **Dependencies**: `reservation-domain` (compile), `spring-tx` (compile), `mockito` + `junit` (test)
- **Pattern consistency**: Follows the same structure as `customer-application` (17 tests, 13 classes) and `fleet-application` — EventPublisher port, command records, manual mapper, NotFoundException in application
