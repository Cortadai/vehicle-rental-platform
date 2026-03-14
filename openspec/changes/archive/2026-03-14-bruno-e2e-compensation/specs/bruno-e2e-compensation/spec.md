# bruno-e2e-compensation Specification

## Purpose
Secuencia E2E ejecutable con `bru run` que valida el compensation flow SAGA: fleet rechaza → payment refund → reservation CANCELLED.

## ADDED Requirements

### Requirement: Compensation sequence in dedicated subfolder
La carpeta `bruno/e2e/compensation/` SHALL contener una secuencia ordenada de requests que validan el compensation flow SAGA.

#### Scenario: Five sequential requests
- **WHEN** se inspecciona la carpeta `bruno/e2e/compensation/`
- **THEN** SHALL contener exactamente 5 ficheros `.bru` con prefijo numerico
- **AND** el prefijo numerico SHALL garantizar el orden de ejecucion con `bru run`

### Requirement: Step 01 — Create customer
El primer paso SHALL crear un customer activo.

#### Scenario: Customer creation
- **WHEN** se ejecuta `01-create-customer.bru`
- **THEN** SHALL enviar POST a `{{customerUrl}}/api/v1/customers`
- **AND** SHALL verificar que el status HTTP es 201
- **AND** SHALL extraer `customerId` como variable de coleccion

### Requirement: Step 02 — Register vehicle
El segundo paso SHALL registrar un vehiculo activo.

#### Scenario: Vehicle registration
- **WHEN** se ejecuta `02-register-vehicle.bru`
- **THEN** SHALL enviar POST a `{{fleetUrl}}/api/v1/vehicles`
- **AND** SHALL verificar que el status HTTP es 201
- **AND** SHALL extraer `vehicleId` como variable de coleccion

### Requirement: Step 03 — Send vehicle to maintenance
El tercer paso SHALL cambiar el estado del vehiculo a MAINTENANCE para provocar el rechazo de fleet.

#### Scenario: Vehicle sent to maintenance
- **WHEN** se ejecuta `03-send-to-maintenance.bru`
- **THEN** SHALL enviar POST a `{{fleetUrl}}/api/v1/vehicles/{{vehicleId}}/maintenance`
- **AND** SHALL verificar que el status HTTP es 200

### Requirement: Step 04 — Create reservation
El cuarto paso SHALL crear una reserva que sera rechazada por fleet.

#### Scenario: Reservation creation with unavailable vehicle
- **WHEN** se ejecuta `04-create-reservation.bru`
- **THEN** SHALL enviar POST a `{{reservationUrl}}/api/v1/reservations` con `customerId` y `vehicleId`
- **AND** SHALL verificar que el status HTTP es 201
- **AND** SHALL verificar que `res.body.data.status` es `PENDING`
- **AND** SHALL extraer `trackingId` como variable de coleccion

### Requirement: Step 05 — Verify CANCELLED after compensation
El quinto paso SHALL esperar a que la SAGA complete la compensation y verificar el estado final.

#### Scenario: Wait and verify CANCELLED status with failure messages
- **WHEN** se ejecuta `05-verify-cancelled.bru`
- **THEN** SHALL esperar al menos 8 segundos para dar tiempo a la compensation cascade
- **AND** SHALL enviar GET a `{{reservationUrl}}/api/v1/reservations/{{trackingId}}`
- **AND** SHALL verificar que el status HTTP es 200
- **AND** SHALL verificar que `res.body.data.status` es `CANCELLED`
- **AND** SHALL verificar que `res.body.data.failureMessages` contiene al menos un mensaje

### Requirement: Happy path reorganized
Los ficheros E2E del happy path SHALL estar en una subcarpeta dedicada.

#### Scenario: Happy path in subfolder
- **WHEN** se inspecciona la carpeta `bruno/e2e/happy-path/`
- **THEN** SHALL contener los 4 ficheros `.bru` del happy path original
- **AND** `bru run --env local e2e/happy-path` SHALL ejecutarlos correctamente
