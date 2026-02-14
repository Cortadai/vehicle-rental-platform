Why
---

The customer-domain module is complete (Aggregate Root, Value Objects, Domain Events, output port), but there's no way to invoke the domain logic. The application layer is the orchestration bridge: it defines use case contracts (input ports), receives commands, delegates to the domain, persists through output ports, and dispatches domain events. Without it, the infrastructure layer (REST controllers, JPA adapters) has nothing to call — controllers can't depend on the domain directly in hexagonal architecture.

This also aligns with docs/17 which prescribes 4 Maven modules per service (domain, **application**, infrastructure, container). The root POM currently declares 3 per service; this change extends the structure by introducing the application module for customer-service.
What Changes
------------

* New Maven module `customer-service/customer-application` with dependency on `customer-domain` and `spring-tx` (for `@Transactional`)
* Root POM updated to declare `customer-application` module
* Input port interfaces: one per use case (Create, Get, Suspend, Activate, Delete)
* Output port interface: `CustomerDomainEventPublisher` for event dispatch after persistence
* Command records for each use case
* Response record for query results
* Application Service implementing all input ports — orchestration only, zero business logic
* Application-level exception for "not found" scenarios (`CustomerNotFoundException`)
* Manual domain-to-DTO mapper (plain Java, no MapStruct yet)
* Unit tests mocking output ports to verify orchestration flow

Capabilities
------------

### New Capabilities

* `customer-application-ports`: Input port interfaces (use case contracts) and `CustomerDomainEventPublisher` output port for event dispatch
* `customer-application-service`: Application Service orchestrating use cases — command handling, transaction boundaries, domain delegation, event dispatch
* `customer-application-dtos`: Command and Response records for all Customer use cases

### Modified Capabilities

* `multi-module-build`: Root POM adds `customer-service/customer-application` module declaration

Impact
------

* **New module**: `customer-service/customer-application/` (POM, source, tests)
* **Root POM**: Add `customer-service/customer-application` to modules list
* **Dependencies**: `customer-infrastructure` (future) → `customer-application` → `customer-domain` → `common`
* **Spring**: Only `spring-tx` for `@Transactional` annotation. No `@Service`, no `@Component` — beans registered manually in container module (future change).
* **CustomerRepository stays in domain**: The output port remains in `customer-domain/port/output/`. Its contract uses only domain types and there is no technical reason to move it. The application module depends on customer-domain and uses the port directly.
