# domain-exceptions

## Purpose

Provides an abstract base exception for all domain-level errors, using business-language error codes instead of HTTP concepts.

## Requirement: DomainException is abstract

DomainException SHALL be an abstract class extending RuntimeException. It cannot be instantiated directly.

## Requirement: Business error code

DomainException SHALL carry a `String errorCode` representing a business-level error (e.g., "CUSTOMER_NOT_FOUND", "INSUFFICIENT_FUNDS"). This code SHALL be accessible via `getErrorCode()`.

### Scenario: Error code accessible

- WHEN a DomainException subclass is created with errorCode "CUSTOMER_NOT_FOUND" and a message
- THEN `getErrorCode()` SHALL return "CUSTOMER_NOT_FOUND"
- AND `getMessage()` SHALL return the provided message

## Requirement: Construction

DomainException SHALL provide two protected constructors:
- `DomainException(String message, String errorCode)` — for exceptions without cause
- `DomainException(String message, String errorCode, Throwable cause)` — for wrapping underlying exceptions

### Scenario: Null errorCode rejected

- WHEN constructed with a null errorCode
- THEN it SHALL throw NullPointerException or IllegalArgumentException

### Scenario: Blank errorCode rejected

- WHEN constructed with a blank errorCode
- THEN it SHALL throw IllegalArgumentException

## Requirement: No HTTP concepts

DomainException SHALL NOT contain any HTTP status code, HTTP status enum, or any reference to HTTP protocol concepts. The mapping from business error codes to HTTP status is the responsibility of the infrastructure layer (GlobalExceptionHandler).

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.common.domain.exception` SHALL import any type from `org.springframework.*`.
