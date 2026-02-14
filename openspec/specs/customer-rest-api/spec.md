customer-rest-api
=================

Purpose
-------

REST input adapter for Customer Service. Exposes all 5 use cases as HTTP endpoints. Includes a controller that delegates to input ports, a request DTO for creation, response wrapping with `ApiResponse`, and a `GlobalExceptionHandler` for consistent error mapping.

## ADDED Requirements

### Requirement: CustomerController exposes all 5 use cases as REST endpoints

CustomerController SHALL be a `@RestController` with base path `/api/v1/customers`. It SHALL delegate all operations to input port interfaces, with zero business logic.

#### Scenario: Create customer endpoint

- **WHEN** `POST /api/v1/customers` is called with a valid `CreateCustomerRequest` body
- **THEN** it SHALL return HTTP 201 Created
- **AND** the response body SHALL be an `ApiResponse<CustomerResponse>`

#### Scenario: Get customer endpoint

- **WHEN** `GET /api/v1/customers/{id}` is called with an existing customer ID
- **THEN** it SHALL return HTTP 200 OK
- **AND** the response body SHALL be an `ApiResponse<CustomerResponse>`

#### Scenario: Get customer not found

- **WHEN** `GET /api/v1/customers/{id}` is called with a non-existing customer ID
- **THEN** it SHALL return HTTP 404 Not Found

#### Scenario: Suspend customer endpoint

- **WHEN** `POST /api/v1/customers/{id}/suspend` is called with an existing active customer ID
- **THEN** it SHALL return HTTP 200 OK

#### Scenario: Activate customer endpoint

- **WHEN** `POST /api/v1/customers/{id}/activate` is called with an existing suspended customer ID
- **THEN** it SHALL return HTTP 200 OK

#### Scenario: Delete customer endpoint

- **WHEN** `DELETE /api/v1/customers/{id}` is called with an existing customer ID
- **THEN** it SHALL return HTTP 204 No Content

#### Scenario: Controller injects input ports not application service

- **WHEN** CustomerController constructor is inspected
- **THEN** it SHALL inject use case interfaces (CreateCustomerUseCase, GetCustomerUseCase, etc.)
- **AND** it SHALL NOT inject CustomerApplicationService directly

### Requirement: CreateCustomerRequest is a REST input DTO

CreateCustomerRequest SHALL be a Java record in the infrastructure layer with fields: firstName (String), lastName (String), email (String), phone (String, nullable). It SHALL have Bean Validation annotations.

#### Scenario: Request DTO has validation annotations

- **WHEN** CreateCustomerRequest is inspected
- **THEN** `firstName` SHALL be annotated with `@NotBlank`
- **AND** `lastName` SHALL be annotated with `@NotBlank`
- **AND** `email` SHALL be annotated with `@NotBlank` and `@Email`

#### Scenario: Request DTO is separate from CreateCustomerCommand

- **WHEN** CreateCustomerRequest is inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.customer.application.dto.command`

### Requirement: GlobalExceptionHandler maps exceptions to HTTP status codes

GlobalExceptionHandler SHALL be a `@RestControllerAdvice` that maps domain and application exceptions to appropriate HTTP responses.

#### Scenario: CustomerNotFoundException maps to 404

- **WHEN** a `CustomerNotFoundException` is thrown during request processing
- **THEN** the response SHALL have HTTP status 404
- **AND** the response body SHALL include the error message

#### Scenario: CustomerDomainException maps to 422

- **WHEN** a `CustomerDomainException` is thrown during request processing
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

All successful responses from CustomerController SHALL use `ApiResponse<T>` from the common module for wrapping response data.

#### Scenario: Successful responses are wrapped

- **WHEN** any successful endpoint returns data
- **THEN** the response SHALL be wrapped in `ApiResponse<CustomerResponse>` with data and metadata

Constraint: Infrastructure layer only
--------------------------------------

All REST classes SHALL live in `com.vehiclerental.customer.infrastructure.adapter.input.rest` or its sub-packages. GlobalExceptionHandler SHALL live in `com.vehiclerental.customer.infrastructure.config`.
