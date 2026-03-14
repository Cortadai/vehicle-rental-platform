## Why

Las migraciones Flyway crean las tablas pero no incluyen indices mas alla de las primary keys y algunos indices estrategicos (outbox_events, saga_state, tracking_id). Columnas de filtrado frecuente como `status` en reservations/payments/vehicles y `customer_id` en reservations no tienen indices. Para un blueprint reutilizable, los indices deben estar — es lo que haria cualquier proyecto real y queda documentado como decision consciente, no como omision.

## What Changes

- Nueva migracion Flyway por servicio (siguiente version disponible) con `CREATE INDEX` para columnas de filtrado frecuente
- Reservation Service: indices en `reservations(customer_id)` y `reservations(status)`
- Payment Service: indice en `payments(status)`
- Fleet Service: indice en `vehicles(status)`
- Customer Service: no necesita indices adicionales — `email` ya tiene indice implicito via UNIQUE constraint, y `status` tiene baja cardinalidad con volumen minimo

## Capabilities

### New Capabilities
- `database-indexes`: Indices de base de datos para columnas de filtrado frecuente en los servicios que lo necesitan

### Modified Capabilities
(ninguna — este change no modifica comportamiento existente, solo rendimiento de queries)

## Impact

- **Ficheros nuevos**: 3 migraciones Flyway (V3 o V4 segun servicio) en reservation, payment y fleet
- **Sin cambios en codigo Java**: los indices son transparentes para JPA/Hibernate
- **Sin impacto en tests**: los indices no afectan al comportamiento funcional
- **Prerequisito**: ninguno — las tablas ya existen
