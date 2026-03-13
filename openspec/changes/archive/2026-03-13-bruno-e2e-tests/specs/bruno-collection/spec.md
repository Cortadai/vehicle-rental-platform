# bruno-collection Specification

## Purpose
Coleccion Bruno versionable en Git con requests para los 4 servicios REST, environments configurados y estructura de carpetas por servicio.

## ADDED Requirements

### Requirement: Bruno collection at project root
La carpeta `bruno/` en la raiz del repositorio SHALL contener una coleccion Bruno valida con `bruno.json` como metadata.

#### Scenario: Collection metadata
- **WHEN** se inspecciona `bruno/bruno.json`
- **THEN** SHALL contener el nombre de la coleccion, version y configuracion basica de Bruno

### Requirement: Local environment configured
La coleccion SHALL incluir un environment `local` con las URLs de los 4 servicios.

#### Scenario: Environment variables for all services
- **WHEN** se inspecciona `bruno/environments/local.bru`
- **THEN** SHALL definir `customerUrl` como `http://localhost:8181`
- **AND** SHALL definir `fleetUrl` como `http://localhost:8182`
- **AND** SHALL definir `reservationUrl` como `http://localhost:8183`
- **AND** SHALL definir `paymentUrl` como `http://localhost:8184`

### Requirement: Customer service requests
La coleccion SHALL incluir requests para todos los endpoints del Customer Service.

#### Scenario: Customer CRUD requests
- **WHEN** se inspecciona la carpeta `bruno/customer-service/`
- **THEN** SHALL contener requests para: create customer (POST), get customer (GET), suspend customer (POST), activate customer (POST), delete customer (DELETE)
- **AND** cada request SHALL usar la variable `{{customerUrl}}` como base URL

### Requirement: Fleet service requests
La coleccion SHALL incluir requests para todos los endpoints del Fleet Service.

#### Scenario: Fleet CRUD requests
- **WHEN** se inspecciona la carpeta `bruno/fleet-service/`
- **THEN** SHALL contener requests para: register vehicle (POST), get vehicle (GET), send to maintenance (POST), activate vehicle (POST), retire vehicle (POST)
- **AND** cada request SHALL usar la variable `{{fleetUrl}}` como base URL

### Requirement: Reservation service requests
La coleccion SHALL incluir requests para todos los endpoints del Reservation Service.

#### Scenario: Reservation requests
- **WHEN** se inspecciona la carpeta `bruno/reservation-service/`
- **THEN** SHALL contener requests para: create reservation (POST), track reservation (GET)
- **AND** cada request SHALL usar la variable `{{reservationUrl}}` como base URL

### Requirement: Payment service requests
La coleccion SHALL incluir requests para todos los endpoints del Payment Service.

#### Scenario: Payment requests
- **WHEN** se inspecciona la carpeta `bruno/payment-service/`
- **THEN** SHALL contener requests para: process payment (POST), refund payment (POST), get payment (GET)
- **AND** cada request SHALL usar la variable `{{paymentUrl}}` como base URL

### Requirement: Requests match API response format
Todos los requests SHALL esperar respuestas envueltas en el wrapper `ApiResponse`.

#### Scenario: ApiResponse wrapper
- **WHEN** un request recibe una respuesta exitosa
- **THEN** el body SHALL tener la estructura `{ "data": { ... }, "meta": { "timestamp": "...", "requestId": "..." } }`
