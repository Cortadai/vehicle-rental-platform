Why
---

The common shared kernel is complete but no service exists yet. Customer Service is the simplest of the 4 services (no SAGA coordination, no cross-service dependencies) and other services (Reservation) need to validate customers. Building the domain layer first lets us learn DDD tactical patterns (Aggregate Root, Value Objects, Domain Events, Typed IDs) in pure Java before adding infrastructure complexity.
What Changes
------------

### Included

* **Parent POM update** — add `customer-service/customer-domain` to root `<modules>` section
* **customer-domain Maven module** — `customer-service/customer-domain/pom.xml`, pure Java, depends only on `common`
* **Customer Aggregate Root** — extends `AggregateRoot<CustomerId>`, factory method `create()`, lifecycle transitions (suspend, activate, delete), domain event registration
* **Value Objects (records)** — `CustomerId` (UUID typed ID), `Email` (format validation), `PhoneNumber` (optional, with validation)
* **Enum** — `CustomerStatus` (ACTIVE, SUSPENDED, DELETED)
* **Domain Events (records)** — `CustomerCreatedEvent`, `CustomerSuspendedEvent`, `CustomerActivatedEvent`, `CustomerDeletedEvent`
* **Domain Exception** — `CustomerDomainException` extending `DomainException` from common
* **Output Port** — `CustomerRepository` interface (findById, save) — the contract that infrastructure will implement later
* **Full test-first coverage** — tests written before implementation, derived from spec scenarios

### Excluded

* **Application layer** (use cases, commands, DTOs) — next change
* **Infrastructure layer** (JPA entities, REST controllers, persistence adapters) — next change
* **Container module** (Spring Boot main, BeanConfiguration) — next change
* **Domain Service** — not needed, all business logic fits within the Customer aggregate
* **Intermediate customer-service parent POM** — the root POM references sub-modules directly (e.g. `customer-service/customer-domain`)

Capabilities
------------

### New Capabilities

* `customer-aggregate`: Customer Aggregate Root with lifecycle management (create, suspend, activate, delete), identity by CustomerId, and domain event emission
* `customer-value-objects`: Typed ID (CustomerId), Email with format validation, PhoneNumber with optional validation
* `customer-domain-events`: Event records for all Customer lifecycle transitions (created, suspended, activated, deleted)
* `customer-repository-port`: Output port interface defining the persistence contract for Customer aggregate

### Modified Capabilities

* `multi-module-build`: Add `customer-service/customer-domain` to root POM modules

Impact
------

1 parent POM update + 1 new pom.xml + ~10 Java classes + ~5 test classes. First service domain module — validates that common shared kernel works in practice and establishes the DDD pattern for the remaining 3 services.
