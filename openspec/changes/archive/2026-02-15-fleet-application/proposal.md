Why
---

The fleet-domain module is complete (Vehicle Aggregate Root, Value Objects, Domain Events, output port), but there's no way to invoke the domain logic. The application layer is the orchestration bridge: it defines use case contracts (input ports), receives commands, delegates to the domain, persists through output ports, and dispatches domain events. Without it, the infrastructure layer (REST controllers, JPA adapters) has nothing to call — controllers can't depend on the domain directly in hexagonal architecture.

This is the second application module in the platform, replicating the proven pattern from customer-application. It follows the same docs/17 four-module structure (domain, **application**, infrastructure, container) and the same architectural decisions: one interface per use case, commands with String types, Application Service as pure orchestration, manual mapper, and `@Transactional` boundaries.

What Changes
------------

### Included

* New Maven module `fleet-service/fleet-application` with dependency on `fleet-domain` and `spring-tx` (for `@Transactional`)
* Root POM updated: add `fleet-service/fleet-application` to `<modules>` and `fleet-application` to `<dependencyManagement>`
* Input port interfaces: one per use case (RegisterVehicle, GetVehicle, SendToMaintenance, Activate, Retire)
* Output port interface: `FleetDomainEventPublisher` for event dispatch after persistence
* Command records for each use case
* Response record for query results (`VehicleResponse`)
* Application Service implementing all input ports — orchestration only, zero business logic
* Application-level exception for "not found" scenarios (`VehicleNotFoundException`)
* Manual domain-to-DTO mapper (plain Java, no MapStruct)
* Unit tests mocking output ports to verify orchestration flow

### Excluded

* Infrastructure layer (JPA entities, REST controllers, Flyway) — separate change `fleet-infrastructure-and-container`
* `@Service` / `@Component` annotations — beans registered manually in future container module
* MapStruct — manual mapper is sufficient for 8 fields
* Update use case (modifying vehicle details) — not in fleet-domain, out of scope
* Pagination/search use cases — belong in infrastructure/application when needed

Capabilities
------------

### New Capabilities

* `fleet-application-ports`: Input port interfaces (use case contracts) for RegisterVehicle, GetVehicle, SendToMaintenance, Activate, Retire, plus `FleetDomainEventPublisher` output port for event dispatch
* `fleet-application-service`: Application Service orchestrating use cases — command handling, transaction boundaries, domain delegation, event dispatch
* `fleet-application-dtos`: Command and Response records for all Vehicle use cases

### Modified Capabilities

* `multi-module-build`: Root POM adds `fleet-service/fleet-application` module declaration and dependencyManagement entry

Impact
------

* **New module**: `fleet-service/fleet-application/` (POM, source, tests)
* **Root POM**: Add `fleet-service/fleet-application` to modules list and `fleet-application` to dependencyManagement
* **Dependencies**: `fleet-infrastructure` (future) → `fleet-application` → `fleet-domain` → `common`
* **Spring**: Only `spring-tx` for `@Transactional` annotation. No `@Service`, no `@Component` — beans registered manually in container module (future change).
* **VehicleRepository stays in domain**: The output port remains in `fleet-domain/port/output/`. Its contract uses only domain types and there is no technical reason to move it. The application module depends on fleet-domain and uses the port directly.
