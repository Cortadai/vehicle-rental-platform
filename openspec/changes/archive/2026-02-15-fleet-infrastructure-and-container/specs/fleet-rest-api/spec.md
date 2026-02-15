fleet-rest-api
==============

Purpose
-------

REST input adapter for Fleet Service. Exposes all 5 use cases as HTTP endpoints. Includes a controller that delegates to input ports, a request DTO for vehicle registration, response wrapping with `ApiResponse`, and a `GlobalExceptionHandler` for consistent error mapping.

## ADDED Requirements

### Requirement: VehicleController exposes all 5 use cases as REST endpoints

VehicleController SHALL be a `@RestController` with base path `/api/v1/vehicles`. It SHALL delegate all operations to input port interfaces, with zero business logic.

#### Scenario: Register vehicle endpoint

- **WHEN** `POST /api/v1/vehicles` is called with a valid `RegisterVehicleRequest` body
- **THEN** it SHALL return HTTP 201 Created
- **AND** the response body SHALL be an `ApiResponse<VehicleResponse>`

#### Scenario: Get vehicle endpoint

- **WHEN** `GET /api/v1/vehicles/{id}` is called with an existing vehicle ID
- **THEN** it SHALL return HTTP 200 OK
- **AND** the response body SHALL be an `ApiResponse<VehicleResponse>`

#### Scenario: Get vehicle not found

- **WHEN** `GET /api/v1/vehicles/{id}` is called with a non-existing vehicle ID
- **THEN** it SHALL return HTTP 404 Not Found

#### Scenario: Send to maintenance endpoint

- **WHEN** `POST /api/v1/vehicles/{id}/maintenance` is called with an existing active vehicle ID
- **THEN** it SHALL return HTTP 200 OK

#### Scenario: Activate vehicle endpoint

- **WHEN** `POST /api/v1/vehicles/{id}/activate` is called with an existing under-maintenance vehicle ID
- **THEN** it SHALL return HTTP 200 OK

#### Scenario: Retire vehicle endpoint

- **WHEN** `POST /api/v1/vehicles/{id}/retire` is called with an existing non-retired vehicle ID
- **THEN** it SHALL return HTTP 200 OK

#### Scenario: Controller injects input ports not application service

- **WHEN** VehicleController constructor is inspected
- **THEN** it SHALL inject use case interfaces (RegisterVehicleUseCase, GetVehicleUseCase, etc.)
- **AND** it SHALL NOT inject FleetApplicationService directly

### Requirement: RegisterVehicleRequest is a REST input DTO

RegisterVehicleRequest SHALL be a Java record in the infrastructure layer with fields: licensePlate (String), make (String), model (String), year (int), category (String), dailyRateAmount (BigDecimal), dailyRateCurrency (String), description (String, nullable). It SHALL have Bean Validation annotations.

#### Scenario: Request DTO has validation annotations

- **WHEN** RegisterVehicleRequest is inspected
- **THEN** `licensePlate` SHALL be annotated with `@NotBlank`
- **AND** `make` SHALL be annotated with `@NotBlank`
- **AND** `model` SHALL be annotated with `@NotBlank`
- **AND** `category` SHALL be annotated with `@NotBlank`
- **AND** `dailyRateAmount` SHALL be annotated with `@NotNull`
- **AND** `dailyRateCurrency` SHALL be annotated with `@NotBlank`

#### Scenario: Request DTO is separate from RegisterVehicleCommand

- **WHEN** RegisterVehicleRequest is inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.fleet.application.dto.command`

### Requirement: GlobalExceptionHandler maps exceptions to HTTP status codes

GlobalExceptionHandler SHALL be a `@RestControllerAdvice` that maps domain and application exceptions to appropriate HTTP responses.

#### Scenario: VehicleNotFoundException maps to 404

- **WHEN** a `VehicleNotFoundException` is thrown during request processing
- **THEN** the response SHALL have HTTP status 404
- **AND** the response body SHALL include the error message

#### Scenario: FleetDomainException maps to 422

- **WHEN** a `FleetDomainException` is thrown during request processing
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

All successful responses from VehicleController SHALL use `ApiResponse<T>` from the common module for wrapping response data.

#### Scenario: Successful responses are wrapped

- **WHEN** any successful endpoint returns data
- **THEN** the response SHALL be wrapped in `ApiResponse<VehicleResponse>` with data and metadata

Constraint: Infrastructure layer only
--------------------------------------

All REST classes SHALL live in `com.vehiclerental.fleet.infrastructure.adapter.input.rest` or its sub-packages. GlobalExceptionHandler SHALL live in `com.vehiclerental.fleet.infrastructure.config`.
