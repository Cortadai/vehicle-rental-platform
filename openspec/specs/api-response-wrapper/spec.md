# api-response-wrapper

## Purpose

Provides a standard envelope for all REST API responses across services, ensuring consistent response format without Spring dependencies.

## Requirement: ApiResponse structure

ApiResponse SHALL be a generic Java record `ApiResponse<T>` with two fields: `T data` and `ApiMetadata meta`.

### Scenario: Create with non-null data

- WHEN `ApiResponse.of(data)` is called with a non-null data object
- THEN it SHALL return an ApiResponse containing that data and auto-generated metadata

### Scenario: Create with null data

- WHEN `ApiResponse.of(null)` is called
- THEN it SHALL return an ApiResponse with null data and auto-generated metadata (valid for DELETE/void responses)

## Requirement: ApiMetadata structure

ApiMetadata SHALL be a Java record with `Instant timestamp` and `String requestId`.

### Scenario: Auto-generated metadata

- WHEN ApiMetadata is created via the `of()` factory
- THEN `timestamp` SHALL be set to the current instant
- AND `requestId` SHALL be a generated UUID string

### Scenario: Null timestamp rejected

- WHEN ApiMetadata is constructed with a null timestamp
- THEN it SHALL throw NullPointerException or IllegalArgumentException

### Scenario: Null or blank requestId rejected

- WHEN ApiMetadata is constructed with a null or blank requestId
- THEN it SHALL throw NullPointerException or IllegalArgumentException

## Requirement: Factory methods

ApiResponse SHALL provide a static factory method `of(T data)` that creates the response with auto-generated ApiMetadata. No public constructor exposure is required beyond the canonical record constructor.

## Requirement: Serialization compatibility

ApiResponse and ApiMetadata SHALL be serializable by Jackson without any Jackson annotations (relying on record accessor methods). This constraint is validated at integration level when services use the response wrapper, not in the common module itself.

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.common.api` SHALL import any type from `org.springframework.*`.
