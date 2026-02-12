## 1. Module Setup

- [x] 1.1 Create `common/pom.xml` — pure Java library POM, inherits from parent, no Spring dependencies, packaging `jar`

## 2. Domain Base Classes (Test-First)

- [x] 2.1 Create `DomainEvent.java` — interface with `UUID eventId()` and `Instant occurredOn()` contract
- [x] 2.2 Create `BaseEntityTest.java` — tests for identity equality (same ID, different ID, null ID reference equality, null ID constant hashCode, null/different type comparison)
- [x] 2.3 Create `AggregateRootTest.java` — tests for event accumulation (register single, multiple in order, defensive copy, clear events)
- [x] 2.4 Create `BaseEntity.java` — abstract generic class `BaseEntity<ID>`, equals/hashCode by ID only
- [x] 2.5 Create `AggregateRoot.java` — abstract generic class extending BaseEntity, `registerDomainEvent()` / `getDomainEvents()` / `clearDomainEvents()`

## 3. Value Objects (Test-First)

- [x] 3.1 Create `MoneyTest.java` — tests for construction validation (null, negative), scale normalization (10.5 → 10.50), arithmetic (add, subtract, multiply), currency mismatch, subtract resulting in negative, multiply by negative, immutability
- [x] 3.2 Create `Money.java` — record with `BigDecimal amount` + `Currency currency`, scale normalization to 2 decimals (RoundingMode.HALF_UP), arithmetic operations returning new instances

## 4. Domain Exceptions

- [x] 4.1 Create `DomainExceptionTest.java` — tests for errorCode accessible, null errorCode rejected, blank errorCode rejected, constructor with cause
- [x] 4.2 Create `DomainException.java` — abstract class extending RuntimeException, String errorCode, two protected constructors, null/blank errorCode validation

## 5. API Response

- [x] 5.1 Create `ApiMetadata.java` — record with `Instant timestamp` + `String requestId`, null/blank validation, static `of()` factory
- [x] 5.2 Create `ApiResponse.java` — generic record `ApiResponse<T>` with `T data` + `ApiMetadata meta`, static `of(T data)` factory with auto-generated metadata

## 6. Verification

- [x] 6.1 Run `mvn clean install -pl common` — compile + all tests pass
- [x] 6.2 Verify zero Spring imports: `grep -r "org.springframework" common/src/main/` returns nothing
