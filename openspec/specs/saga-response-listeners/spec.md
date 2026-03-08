## ADDED Requirements

### Requirement: CustomerValidatedResponseListener

CustomerValidatedResponseListener SHALL be a `@Component` in `reservation-infrastructure/adapter/input/messaging/saga/` that listens on `customer.validated.queue`. It SHALL parse the message body as JSON, extract `reservationId`, and call `orchestrator.handleStepSuccess(reservationId, "CUSTOMER_VALIDATION")`.

#### Scenario: Successful customer validation received

- **WHEN** a message arrives on `customer.validated.queue` with payload `{ "customerId": "...", "reservationId": "uuid" }`
- **THEN** the listener SHALL call `handleStepSuccess(reservationId, "CUSTOMER_VALIDATION")` on the orchestrator

### Requirement: CustomerRejectedResponseListener

CustomerRejectedResponseListener SHALL listen on `customer.rejected.queue`. It SHALL parse the message body, extract `reservationId` and `failureMessages`, and call `orchestrator.handleStepFailure(reservationId, "CUSTOMER_VALIDATION", failureMessages)`.

#### Scenario: Customer rejection received

- **WHEN** a message arrives on `customer.rejected.queue` with payload `{ "customerId": "...", "reservationId": "uuid", "failureMessages": ["reason"] }`
- **THEN** the listener SHALL call `handleStepFailure(reservationId, "CUSTOMER_VALIDATION", failureMessages)` on the orchestrator

### Requirement: PaymentCompletedResponseListener

PaymentCompletedResponseListener SHALL listen on `payment.completed.queue`. It SHALL parse the message body, extract `reservationId`, and call `orchestrator.handleStepSuccess(reservationId, "PAYMENT")`.

#### Scenario: Successful payment received

- **WHEN** a message arrives on `payment.completed.queue` with payload `{ "paymentId": "...", "reservationId": "uuid", "customerId": "...", "amount": {...} }`
- **THEN** the listener SHALL call `handleStepSuccess(reservationId, "PAYMENT")` on the orchestrator

### Requirement: PaymentFailedResponseListener

PaymentFailedResponseListener SHALL listen on `payment.failed.queue`. It SHALL parse the message body, extract `reservationId` and `failureMessages`, and call `orchestrator.handleStepFailure(reservationId, "PAYMENT", failureMessages)`.

#### Scenario: Payment failure received

- **WHEN** a message arrives on `payment.failed.queue` with payload `{ "paymentId": "...", "reservationId": "uuid", "failureMessages": ["reason"] }`
- **THEN** the listener SHALL call `handleStepFailure(reservationId, "PAYMENT", failureMessages)` on the orchestrator

### Requirement: PaymentRefundedResponseListener

PaymentRefundedResponseListener SHALL listen on `payment.refunded.queue`. It SHALL parse the message body, extract `reservationId`, and call `orchestrator.handleCompensationComplete(reservationId, "PAYMENT")`.

#### Scenario: Payment refund confirmation received

- **WHEN** a message arrives on `payment.refunded.queue` with payload `{ "paymentId": "...", "reservationId": "uuid", "amount": {...} }`
- **THEN** the listener SHALL call `handleCompensationComplete(reservationId, "PAYMENT")` on the orchestrator

### Requirement: FleetConfirmedResponseListener

FleetConfirmedResponseListener SHALL listen on `fleet.confirmed.queue`. It SHALL parse the message body, extract `reservationId`, and call `orchestrator.handleStepSuccess(reservationId, "FLEET_CONFIRMATION")`.

#### Scenario: Fleet confirmation received

- **WHEN** a message arrives on `fleet.confirmed.queue` with payload `{ "vehicleId": "...", "reservationId": "uuid" }`
- **THEN** the listener SHALL call `handleStepSuccess(reservationId, "FLEET_CONFIRMATION")` on the orchestrator

### Requirement: FleetRejectedResponseListener

FleetRejectedResponseListener SHALL listen on `fleet.rejected.queue`. It SHALL parse the message body, extract `reservationId` and `failureMessages`, and call `orchestrator.handleStepFailure(reservationId, "FLEET_CONFIRMATION", failureMessages)`.

#### Scenario: Fleet rejection received

- **WHEN** a message arrives on `fleet.rejected.queue` with payload `{ "vehicleId": "...", "reservationId": "uuid", "failureMessages": ["reason"] }`
- **THEN** the listener SHALL call `handleStepFailure(reservationId, "FLEET_CONFIRMATION", failureMessages)` on the orchestrator

### Requirement: All listeners parse raw AMQP messages

All 7 listeners SHALL receive `org.springframework.amqp.core.Message` as parameter and parse the body as JSON using `ObjectMapper.readTree()`. This follows the same pattern as existing listeners in Customer, Fleet, and Payment services.

#### Scenario: Consistent message parsing pattern

- **WHEN** any SAGA response listener receives a message
- **THEN** it SHALL parse `message.getBody()` using `ObjectMapper.readTree()`
- **AND** extract fields via `json.get("fieldName").asText()` or `.decimalValue()`

### Requirement: Listeners log received messages

All listeners SHALL log the received message at INFO level including the reservationId and the step result (success/failure).

#### Scenario: Success response logged

- **WHEN** a success response is received
- **THEN** the listener SHALL log at INFO level with reservationId and step name

#### Scenario: Failure response logged

- **WHEN** a failure response is received
- **THEN** the listener SHALL log at WARN level with reservationId, step name, and failure messages
