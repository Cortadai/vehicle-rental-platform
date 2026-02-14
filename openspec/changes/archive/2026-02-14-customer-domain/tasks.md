## 1. Module Setup

- [x] 1.1 Create `customer-service/customer-domain/pom.xml` — inherits from root parent POM, depends on `common`, zero Spring dependencies in compile scope, packaging `jar`
- [x] 1.2 Create directory structure under `customer-service/customer-domain/src/main/java/com/vehiclerental/customer/domain/` with subdirectories: `model/aggregate/`, `model/vo/`, `event/`, `exception/`, `port/output/`
- [x] 1.3 Create test directory structure under `customer-service/customer-domain/src/test/java/com/vehiclerental/customer/domain/` with subdirectories: `model/aggregate/`, `model/vo/`, `event/`, `exception/`

## 2. Value Objects and Enum (Test-First)

- [x] 2.1 Create `CustomerIdTest.java` — tests for valid construction, null UUID rejected (throws CustomerDomainException), equality by value
- [x] 2.2 Create `CustomerId.java` — record wrapping UUID, null validation in compact constructor
- [x] 2.3 Create `EmailTest.java` — tests for valid email, subdomains, null rejected, blank rejected, missing @, missing domain, missing local part (all throw CustomerDomainException)
- [x] 2.4 Create `Email.java` — record wrapping String, practical regex validation in compact constructor
- [x] 2.5 Create `PhoneNumberTest.java` — tests for valid phone, parentheses, null rejected, blank rejected, too short (<3), too long (>20), letters rejected (all throw CustomerDomainException)
- [x] 2.6 Create `PhoneNumber.java` — record wrapping String, format and length validation in compact constructor
- [x] 2.7 Create `CustomerStatus.java` — enum with ACTIVE, SUSPENDED, DELETED

## 3. Domain Exception

- [x] 3.1 Create `CustomerDomainExceptionTest.java` — tests for errorCode accessible, message accessible, constructor with cause
- [x] 3.2 Create `CustomerDomainException.java` — extends DomainException from common, constructors requiring errorCode

## 4. Domain Events (Test-First)

- [x] 4.1 Create `CustomerDomainEventsTest.java` — tests for: CustomerCreatedEvent fields accessible (customerId, firstName, lastName, email), null eventId throws CustomerDomainException, null occurredOn throws CustomerDomainException, all events implement DomainEvent, lifecycle events (Suspended, Activated, Deleted) carry customerId with non-null eventId/occurredOn
- [x] 4.2 Create `CustomerCreatedEvent.java` — record implementing DomainEvent with customerId, firstName, lastName, email; null validation for eventId and occurredOn (throws CustomerDomainException)
- [x] 4.3 Create `CustomerSuspendedEvent.java` — record implementing DomainEvent with customerId; null validation
- [x] 4.4 Create `CustomerActivatedEvent.java` — record implementing DomainEvent with customerId; null validation
- [x] 4.5 Create `CustomerDeletedEvent.java` — record implementing DomainEvent with customerId; null validation

## 5. Customer Aggregate Root (Test-First)

- [x] 5.1 Create `CustomerTest.java` — tests for: successful creation (status ACTIVE, CustomerId generated, CustomerCreatedEvent emitted), null/blank firstName rejected, null/blank lastName rejected, null email rejected, null phone accepted, fields accessible after creation, reconstruct does not emit events, no public constructors
- [x] 5.2 Create `CustomerLifecycleTest.java` — tests for: suspend active (SUSPENDED + event), suspend non-active throws, activate suspended (ACTIVE + event), activate non-suspended throws, delete active (DELETED + event), delete suspended (DELETED + event), delete already deleted throws, isActive true/false, exception carries errorCode, exception message includes current state
- [x] 5.3 Create `Customer.java` — Aggregate Root extending AggregateRoot<CustomerId>, private constructor, `create()` factory method, `reconstruct()` factory method, `suspend()`, `activate()`, `delete()`, `isActive()`, getters only

## 6. Output Port

- [x] 6.1 Create `CustomerRepository.java` — interface with `save(Customer)` returning Customer and `findById(CustomerId)` returning `Optional<Customer>`, domain types only

## 7. Verification

- [x] 7.1 Run `mvn clean install` from `customer-service/customer-domain/` — compile + all tests pass
- [x] 7.2 Verify zero Spring imports: `grep -r "org.springframework" customer-service/customer-domain/src/main/` returns nothing
