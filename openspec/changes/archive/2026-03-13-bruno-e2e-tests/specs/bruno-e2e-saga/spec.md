# bruno-e2e-saga Specification

## Purpose
Secuencia E2E ejecutable con `bru run` que valida el happy path SAGA completo: crear datos de setup, lanzar la reserva y verificar que alcanza el estado CONFIRMED.

## ADDED Requirements

### Requirement: E2E sequence in dedicated folder
La carpeta `bruno/e2e/` SHALL contener una secuencia ordenada de requests que validan el happy path SAGA.

#### Scenario: Four sequential requests
- **WHEN** se inspecciona la carpeta `bruno/e2e/`
- **THEN** SHALL contener exactamente 4 ficheros `.bru` con prefijo numerico: `01-create-customer.bru`, `02-register-vehicle.bru`, `03-create-reservation.bru`, `04-verify-confirmed.bru`
- **AND** el prefijo numerico SHALL garantizar el orden de ejecucion con `bru run`

### Requirement: Step 01 — Create customer
El primer paso SHALL crear un customer y extraer su ID para uso posterior.

#### Scenario: Customer creation and ID extraction
- **WHEN** se ejecuta `01-create-customer.bru`
- **THEN** SHALL enviar POST a `{{customerUrl}}/api/v1/customers` con firstName, lastName, email y phone
- **AND** SHALL verificar que el status HTTP es 201
- **AND** el script post-response SHALL extraer `customerId` de `res.body.data.customerId` y guardarlo como variable de coleccion

### Requirement: Step 02 — Register vehicle
El segundo paso SHALL registrar un vehiculo y extraer su ID.

#### Scenario: Vehicle registration and ID extraction
- **WHEN** se ejecuta `02-register-vehicle.bru`
- **THEN** SHALL enviar POST a `{{fleetUrl}}/api/v1/vehicles` con licensePlate, make, model, year, category, dailyRateAmount y dailyRateCurrency
- **AND** SHALL verificar que el status HTTP es 201
- **AND** el script post-response SHALL extraer `vehicleId` de `res.body.data.vehicleId` y guardarlo como variable de coleccion

### Requirement: Step 03 — Create reservation
El tercer paso SHALL crear una reserva usando los IDs de los pasos anteriores y verificar el estado inicial.

#### Scenario: Reservation creation with chained variables
- **WHEN** se ejecuta `03-create-reservation.bru`
- **THEN** SHALL enviar POST a `{{reservationUrl}}/api/v1/reservations` con `customerId` y `vehicleId` de las variables de coleccion
- **AND** el body SHALL incluir pickupAddress, pickupCity, returnAddress, returnCity, pickupDate, returnDate, currency y un item con vehicleId, dailyRate y days
- **AND** SHALL verificar que el status HTTP es 201
- **AND** SHALL verificar que `res.body.data.status` es `PENDING`
- **AND** el script post-response SHALL extraer `trackingId` de `res.body.data.trackingId` y guardarlo como variable de coleccion

### Requirement: Step 04 — Verify CONFIRMED after SAGA
El cuarto paso SHALL esperar a que la SAGA complete y verificar el estado final.

#### Scenario: Wait and verify CONFIRMED status
- **WHEN** se ejecuta `04-verify-confirmed.bru`
- **THEN** el script pre-request SHALL esperar 5 segundos para dar tiempo a la SAGA
- **AND** SHALL enviar GET a `{{reservationUrl}}/api/v1/reservations/{{trackingId}}`
- **AND** SHALL verificar que el status HTTP es 200
- **AND** SHALL verificar que `res.body.data.status` es `CONFIRMED`

### Requirement: E2E executable via bru run
La secuencia E2E completa SHALL ser ejecutable con un solo comando de Bruno CLI.

#### Scenario: Full E2E run
- **WHEN** se ejecuta `bru run --env local` desde la carpeta `bruno/e2e/`
- **THEN** los 4 requests SHALL ejecutarse en orden numerico
- **AND** los 4 requests SHALL completarse con todas las aserciones pasando
- **AND** el resultado final SHALL mostrar 4/4 tests passed

### Requirement: E2E requires running platform
La secuencia E2E SHALL requerir que la plataforma este corriendo en Docker Compose.

#### Scenario: Platform prerequisite
- **WHEN** se intenta ejecutar `bru run` sin los servicios corriendo
- **THEN** los requests SHALL fallar con errores de conexion
