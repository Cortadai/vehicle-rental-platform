# reservation-rest-api Specification

## Purpose

REST input adapter for Reservation Service. Exposes 2 use cases (create and track) as HTTP endpoints. Includes a controller that delegates to input ports, a request DTO with nested item record and Jakarta validation, response wrapping with `ApiResponse`, and a `GlobalExceptionHandler` for consistent error mapping.

## ADDED Requirements

### Requirement: ReservationController exposes 2 use cases as REST endpoints

ReservationController SHALL be a `@RestController` with base path `/api/v1/reservations`. It SHALL delegate all operations to input port interfaces, with zero business logic.

#### Scenario: Create reservation endpoint

- **WHEN** `POST /api/v1/reservations` is called with a valid `CreateReservationRequest` body
- **THEN** it SHALL return HTTP 201 Created
- **AND** the response body SHALL be an `ApiResponse<CreateReservationResponse>`

#### Scenario: Track reservation endpoint

- **WHEN** `GET /api/v1/reservations/{trackingId}` is called with an existing tracking ID
- **THEN** it SHALL return HTTP 200 OK
- **AND** the response body SHALL be an `ApiResponse<TrackReservationResponse>`

#### Scenario: Track reservation not found

- **WHEN** `GET /api/v1/reservations/{trackingId}` is called with a non-existing tracking ID
- **THEN** it SHALL return HTTP 404 Not Found

#### Scenario: Controller injects input ports not application service

- **WHEN** ReservationController constructor is inspected
- **THEN** it SHALL inject use case interfaces (CreateReservationUseCase, TrackReservationUseCase)
- **AND** it SHALL NOT inject ReservationApplicationService directly

#### Scenario: Create endpoint converts request to command

- **WHEN** `POST /api/v1/reservations` is called with a valid `CreateReservationRequest`
- **THEN** the controller SHALL convert the request DTO to a `CreateReservationCommand` (including nested items conversion)
- **AND** it SHALL pass the command to `CreateReservationUseCase.execute()`

#### Scenario: Track endpoint converts path variable to command

- **WHEN** `GET /api/v1/reservations/{trackingId}` is called
- **THEN** the controller SHALL create a `TrackReservationCommand` from the path variable
- **AND** it SHALL pass the command to `TrackReservationUseCase.execute()`

### Requirement: CreateReservationRequest is a REST input DTO with nested items

CreateReservationRequest SHALL be a Java record in the infrastructure layer with fields: customerId (String), pickupAddress (String), pickupCity (String), returnAddress (String), returnCity (String), pickupDate (String), returnDate (String), currency (String), items (List\<CreateReservationItemRequest\>). It SHALL have Jakarta Bean Validation annotations.

#### Scenario: Request DTO has validation annotations

- **WHEN** CreateReservationRequest is inspected
- **THEN** `customerId` SHALL be annotated with `@NotBlank`
- **AND** `pickupAddress` SHALL be annotated with `@NotBlank`
- **AND** `pickupCity` SHALL be annotated with `@NotBlank`
- **AND** `returnAddress` SHALL be annotated with `@NotBlank`
- **AND** `returnCity` SHALL be annotated with `@NotBlank`
- **AND** `pickupDate` SHALL be annotated with `@NotBlank`
- **AND** `returnDate` SHALL be annotated with `@NotBlank`
- **AND** `currency` SHALL be annotated with `@NotBlank`
- **AND** `items` SHALL be annotated with `@NotEmpty` and `@Valid`

#### Scenario: Inner item request DTO has validation annotations

- **WHEN** CreateReservationItemRequest is inspected
- **THEN** it SHALL be a Java record with fields: vehicleId (String), dailyRate (BigDecimal), days (int)
- **AND** `vehicleId` SHALL be annotated with `@NotBlank`
- **AND** `dailyRate` SHALL be annotated with `@NotNull` and `@Positive`
- **AND** `days` SHALL be annotated with `@Positive`

#### Scenario: Request DTO is separate from CreateReservationCommand

- **WHEN** CreateReservationRequest is inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.reservation.application.dto.command`

### Requirement: GlobalExceptionHandler maps exceptions to HTTP status codes

GlobalExceptionHandler SHALL be a `@RestControllerAdvice` that maps domain and application exceptions to appropriate HTTP responses.

#### Scenario: ReservationNotFoundException maps to 404

- **WHEN** a `ReservationNotFoundException` is thrown during request processing
- **THEN** the response SHALL have HTTP status 404
- **AND** the response body SHALL include the error message

#### Scenario: ReservationDomainException maps to 422

- **WHEN** a `ReservationDomainException` is thrown during request processing
- **THEN** the response SHALL have HTTP status 422
- **AND** the response body SHALL include the error code and message

#### Scenario: Validation errors map to 400

- **WHEN** a `MethodArgumentNotValidException` is thrown (invalid request body)
- **THEN** the response SHALL have HTTP status 400
- **AND** the response body SHALL include field-level validation error details

#### Scenario: Unexpected exceptions map to 500

- **WHEN** an unexpected `Exception` is thrown
- **THEN** the response SHALL have HTTP status 500
- **AND** the response body SHALL NOT expose internal details (stack trace, class names)

### Requirement: API responses use ApiResponse wrapper

All successful responses from ReservationController SHALL use `ApiResponse<T>` from the common module for wrapping response data.

#### Scenario: Successful responses are wrapped

- **WHEN** any successful endpoint returns data
- **THEN** the response SHALL be wrapped in `ApiResponse<T>` with data and metadata

## Constraint: Infrastructure layer only

All REST classes SHALL live in `com.vehiclerental.reservation.infrastructure.adapter.input.rest` or its sub-packages. GlobalExceptionHandler SHALL live in `com.vehiclerental.reservation.infrastructure.config`.
