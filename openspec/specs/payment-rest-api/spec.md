payment-rest-api
================

Purpose
-------

REST input adapter for Payment Service. Exposes 3 use cases as HTTP endpoints. Includes a controller that delegates to input ports, request DTOs for processing and refund, response wrapping with `ApiResponse`, and a `GlobalExceptionHandler` for consistent error mapping.

## ADDED Requirements

### Requirement: PaymentController exposes 3 use cases as REST endpoints

PaymentController SHALL be a `@RestController` with base path `/api/v1/payments`. It SHALL delegate all operations to input port interfaces, with zero business logic.

#### Scenario: Process payment endpoint

- **WHEN** `POST /api/v1/payments` is called with a valid `ProcessPaymentRequest` body
- **THEN** it SHALL convert the request to a `ProcessPaymentCommand`
- **AND** it SHALL delegate to `ProcessPaymentUseCase.execute()`
- **AND** it SHALL return HTTP 201 Created
- **AND** the response body SHALL be an `ApiResponse<PaymentResponse>`

#### Scenario: Refund payment endpoint

- **WHEN** `POST /api/v1/payments/refund` is called with a valid `RefundPaymentRequest` body
- **THEN** it SHALL convert the request to a `RefundPaymentCommand`
- **AND** it SHALL delegate to `RefundPaymentUseCase.execute()`
- **AND** it SHALL return HTTP 200 OK
- **AND** the response body SHALL be an `ApiResponse<PaymentResponse>`

#### Scenario: Get payment endpoint

- **WHEN** `GET /api/v1/payments/{id}` is called with an existing payment ID
- **THEN** it SHALL delegate to `GetPaymentUseCase.execute()`
- **AND** it SHALL return HTTP 200 OK
- **AND** the response body SHALL be an `ApiResponse<PaymentResponse>`

#### Scenario: Get payment not found

- **WHEN** `GET /api/v1/payments/{id}` is called with a non-existing payment ID
- **THEN** it SHALL return HTTP 404 Not Found

#### Scenario: Controller injects input ports not application service

- **WHEN** PaymentController constructor is inspected
- **THEN** it SHALL inject use case interfaces (`ProcessPaymentUseCase`, `RefundPaymentUseCase`, `GetPaymentUseCase`)
- **AND** it SHALL NOT inject `PaymentApplicationService` directly

### Requirement: ProcessPaymentRequest is a REST input DTO

ProcessPaymentRequest SHALL be a Java record in the infrastructure layer with fields: `reservationId` (String), `customerId` (String), `amount` (BigDecimal), `currency` (String). It SHALL have Bean Validation annotations.

#### Scenario: Request DTO has validation annotations

- **WHEN** ProcessPaymentRequest is inspected
- **THEN** `reservationId` SHALL be annotated with `@NotBlank`
- **AND** `customerId` SHALL be annotated with `@NotBlank`
- **AND** `amount` SHALL be annotated with `@NotNull`
- **AND** `currency` SHALL be annotated with `@NotBlank`

#### Scenario: Request DTO is separate from ProcessPaymentCommand

- **WHEN** ProcessPaymentRequest is inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.payment.application.dto.command`

### Requirement: RefundPaymentRequest is a REST input DTO

RefundPaymentRequest SHALL be a Java record in the infrastructure layer with field: `reservationId` (String). It SHALL have Bean Validation annotations.

#### Scenario: Request DTO has validation annotations

- **WHEN** RefundPaymentRequest is inspected
- **THEN** `reservationId` SHALL be annotated with `@NotBlank`

#### Scenario: Request DTO is separate from RefundPaymentCommand

- **WHEN** RefundPaymentRequest is inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.payment.application.dto.command`

### Requirement: GlobalExceptionHandler maps exceptions to HTTP status codes

GlobalExceptionHandler SHALL be a `@RestControllerAdvice` that maps domain and application exceptions to appropriate HTTP responses.

#### Scenario: PaymentNotFoundException maps to 404

- **WHEN** a `PaymentNotFoundException` is thrown during request processing
- **THEN** the response SHALL have HTTP status 404
- **AND** the response body SHALL include the error message

#### Scenario: PaymentDomainException maps to 422

- **WHEN** a `PaymentDomainException` is thrown during request processing
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

All successful responses from PaymentController SHALL use `ApiResponse<T>` from the common module for wrapping response data.

#### Scenario: Successful responses are wrapped

- **WHEN** any successful endpoint returns data
- **THEN** the response SHALL be wrapped in `ApiResponse<PaymentResponse>` with data and metadata

Constraint: Infrastructure layer only
--------------------------------------

All REST classes SHALL live in `com.vehiclerental.payment.infrastructure.adapter.input.rest` or its sub-packages. GlobalExceptionHandler SHALL live in `com.vehiclerental.payment.infrastructure.config`.
