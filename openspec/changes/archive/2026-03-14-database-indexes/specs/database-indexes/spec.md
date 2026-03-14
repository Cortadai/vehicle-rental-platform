# database-indexes Specification

## Purpose
Indices de base de datos para columnas de filtrado frecuente en reservation, payment y fleet services.

## ADDED Requirements

### Requirement: Reservation service indexes
La base de datos del reservation service SHALL tener indices en columnas de filtrado frecuente.

#### Scenario: Index on customer_id
- **WHEN** se inspecciona el schema de la tabla `reservations`
- **THEN** SHALL existir un indice `idx_reservations_customer_id` en la columna `customer_id`

#### Scenario: Index on status
- **WHEN** se inspecciona el schema de la tabla `reservations`
- **THEN** SHALL existir un indice `idx_reservations_status` en la columna `status`

### Requirement: Payment service indexes
La base de datos del payment service SHALL tener indice en la columna status.

#### Scenario: Index on status
- **WHEN** se inspecciona el schema de la tabla `payments`
- **THEN** SHALL existir un indice `idx_payments_status` en la columna `status`

### Requirement: Fleet service indexes
La base de datos del fleet service SHALL tener indice en la columna status.

#### Scenario: Index on status
- **WHEN** se inspecciona el schema de la tabla `vehicles`
- **THEN** SHALL existir un indice `idx_vehicles_status` en la columna `status`

### Requirement: Idempotent migrations
Todas las migraciones de indices SHALL ser idempotentes.

#### Scenario: Re-runnable index creation
- **WHEN** se ejecuta la migracion de indices
- **THEN** SHALL usar `CREATE INDEX IF NOT EXISTS`
- **AND** SHALL no fallar si el indice ya existe
