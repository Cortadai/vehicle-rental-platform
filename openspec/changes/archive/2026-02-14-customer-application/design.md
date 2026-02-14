Context
-------

The customer-domain module is complete with: Customer aggregate root (create, reconstruct, suspend, activate, delete), typed Value Objects (CustomerId, Email, PhoneNumber), domain events (Created, Suspended, Activated, Deleted), CustomerDomainException, and a CustomerRepository output port interface. The domain has 58 passing tests and zero Spring dependencies.

The application layer bridges the domain with the outside world. Per docs/17, it's a separate Maven module (`customer-application`) that depends on `customer-domain` and adds only `spring-tx` for `@Transactional`. Infrastructure adapters (future change) will depend on this module to call use cases through input ports.

The existing root POM declares 3 modules per service (domain, infrastructure, container), but docs/17 prescribes 4 (adding application). This change extends the structure for customer-service.
Goals / Non-Goals
-----------------

**Goals:**

* Establish the application layer pattern for all services to follow
* Define input ports (one interface per use case) with command/response DTOs
* Add CustomerDomainEventPublisher output port for event dispatch
* Implement CustomerApplicationService as pure orchestrator (zero business logic)
* Add CustomerNotFoundException for "not found" scenarios (application-level exception)
* Add CustomerApplicationMapper (manual, plain Java)
* Unit test the application service with mocked output ports
* Update root POM to include customer-application module

**Non-Goals:**

* Domain Service — Customer doesn't need cross-aggregate coordination
* SAGA step implementation — belongs in a future change when Reservation Service coordinates
* Outbox scheduler — belongs in infrastructure layer
* MapStruct mapper — manual mapper is sufficient, MapStruct adds complexity without benefit at this scale
* Input validation on commands — validation happens in domain Value Objects (Email, PhoneNumber constructors)
* Email uniqueness validation — belongs in infrastructure/persistence layer, out of scope for application
* Relocating CustomerRepository — the output port stays in domain where it was placed in customer-domain change

Decisions
---------

### Decision 1: CustomerRepository stays in domain

**Choice**: Leave `CustomerRepository` in `customer-domain/port/output/`. The application module depends on customer-domain and uses the port directly.

**Rationale**: The repository interface contract uses only domain types (`Customer`, `CustomerId`). There is no technical reason to move it — domain has zero dependencies, so the interface compiles and lives cleanly there. Moving it would add noise to this change for no functional benefit. If the port ever needs application-layer types (e.g., pagination DTOs), it can be relocated via a clean refactor.

**Alternative rejected**: Move to `customer-application/port/output/`. While aligned with docs/17 layout, the pragmatic decision is to avoid unnecessary churn. The interface is framework-free regardless of its location.

### Decision 2: One Application Service implementing all input ports

**Choice**: A single `CustomerApplicationService` implements all 5 use case interfaces (Create, Get, Suspend, Activate, Delete).

**Rationale**: Customer has a small number of use cases (5) with minimal complexity. Splitting into 5 separate service classes would add file count without benefit. If a use case grows complex (e.g., SAGA involvement), it can be extracted later.

**Alternative rejected**: One service class per use case. Over-engineering for 5 simple orchestration methods.

### Decision 3: Commands use String for IDs, not CustomerId

**Choice**: Command records carry `String customerId` (not `CustomerId`). The Application Service converts to `CustomerId` via `new CustomerId(UUID.fromString(...))`.

**Rationale**: Commands are the boundary between the outside world (REST controllers, message listeners) and the domain. The outside world speaks in strings/UUIDs. Converting to typed IDs is the application layer's responsibility — it's the "anti-corruption" translation.

**Alternative rejected**: Commands with `CustomerId`. Would leak domain types into the infrastructure layer's request handling.

### Decision 4: CustomerNotFoundException extends RuntimeException (application-level exception)

**Choice**: `CustomerNotFoundException` extends `RuntimeException` directly, NOT `CustomerDomainException`. It lives in the application module under `exception/`. It carries the customer ID as a String for diagnostic purposes and provides a descriptive message.

**Rationale**: "Not found" is an application-level concern — the domain doesn't know about persistence queries or resource lookup. Domain exceptions represent invariant violations (invalid state transitions, validation failures). Keeping them separate lets the infrastructure's GlobalExceptionHandler distinguish between domain errors (→ 422 Unprocessable Entity) and application errors (→ 404 Not Found) cleanly.

**Alternative rejected**: Extend `CustomerDomainException`. Would conflate two different categories of errors (domain invariant violations vs. application-level "not found") and make it harder for exception handlers to map to appropriate HTTP status codes.

### Decision 5: Manual mapper instead of MapStruct

**Choice**: `CustomerApplicationMapper` is a plain Java class with a `toResponse(Customer)` method. No MapStruct, no annotations.

**Rationale**: Customer has one aggregate with 7 fields. A manual mapper is ~15 lines. MapStruct adds annotation processor complexity, binding configuration, and a generated class — all for mapping 7 fields. MapStruct pays off with 10+ entities and complex mappings.

**Alternative rejected**: MapStruct. Overhead exceeds benefit for this simple case.

### Decision 6: Lifecycle use cases return void

**Choice**: Suspend, Activate, and Delete use cases return `void`, not `CustomerResponse`.

**Rationale**: These are command operations (CQS principle). The caller doesn't need the updated state — if they do, they can issue a separate get query. This keeps the API semantically clean.

**Alternative rejected**: Return `CustomerResponse` from all mutations. Violates CQS and couples query needs to command execution.
Package Structure
-----------------

    customer-service/customer-application/src/main/java/com/vehiclerental/customer/application/
    ├── port/
    │   ├── input/
    │   │   ├── CreateCustomerUseCase.java
    │   │   ├── GetCustomerUseCase.java
    │   │   ├── SuspendCustomerUseCase.java
    │   │   ├── ActivateCustomerUseCase.java
    │   │   └── DeleteCustomerUseCase.java
    │   └── output/
    │       └── CustomerDomainEventPublisher.java
    ├── service/
    │   └── CustomerApplicationService.java
    ├── dto/
    │   ├── command/
    │   │   ├── CreateCustomerCommand.java
    │   │   ├── GetCustomerCommand.java
    │   │   ├── SuspendCustomerCommand.java
    │   │   ├── ActivateCustomerCommand.java
    │   │   └── DeleteCustomerCommand.java
    │   └── response/
    │       └── CustomerResponse.java
    ├── mapper/
    │   └── CustomerApplicationMapper.java
    └── exception/
        └── CustomerNotFoundException.java

Risks / Trade-offs
------------------

* **spring-tx dependency in application** — Purists argue `@Transactional` couples the application layer to Spring. Mitigation: it's a single annotation import, no runtime dependency on Spring Context. The alternative (TransactionPort interface) adds complexity for zero practical benefit.
* **Single Application Service may not scale** — If SAGA steps or complex validation are added later, the single class could grow. Mitigation: each use case is a separate method behind its own interface, so extraction is a mechanical refactor.
* **CustomerNotFoundException as RuntimeException** — Unlike domain exceptions which share a common hierarchy, this exception stands alone. If more application-level exceptions appear, a common `CustomerApplicationException` base could be introduced. For now, one exception doesn't justify the hierarchy.
