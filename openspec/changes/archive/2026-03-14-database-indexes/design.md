## Context

La plataforma tiene 4 servicios con PostgreSQL. Las migraciones Flyway crean tablas con PKs, algunos indices estrategicos (outbox_events, saga_state) y UNIQUE constraints que implican indices en PostgreSQL. Faltan indices en columnas de filtrado frecuente: status y customer_id.

## Goals / Non-Goals

**Goals:**
- Indices en columnas de filtrado frecuente (status, customer_id)
- Migraciones Flyway idempotentas (IF NOT EXISTS)
- Documentar que indices ya cubren el resto (UNIQUE, outbox, saga)

**Non-Goals:**
- Indices compuestos o parciales — no hay queries complejas en el POC
- Analisis de cardinalidad o EXPLAIN — sin carga real no aporta
- Customer service — email ya indexado via UNIQUE, status con volumen minimo

## Decisions

### D1: Solo 3 servicios, no 4

**Decision:** Customer service no recibe migracion. El UNIQUE constraint en `email` ya crea indice implicito en PostgreSQL, y `status` tiene cardinalidad tan baja (2 valores: ACTIVE/SUSPENDED) que un indice no aporta con volumen minimo.

**Rationale:** Añadir un indice innecesario ensucia el schema sin beneficio. Mejor documentar por que NO se añade que añadirlo por simetria.

### D2: IF NOT EXISTS en todos los CREATE INDEX

**Decision:** Usar `CREATE INDEX IF NOT EXISTS` para idempotencia.

**Rationale:** Evita fallos si la migracion se re-ejecuta o si algun entorno ya tiene el indice. Es buena practica en migraciones Flyway.

### D3: Naming convention — idx_{tabla}_{columna}

**Decision:** Seguir el patron existente: `idx_outbox_events_status_created`, `idx_reservations_tracking_id`, `idx_saga_state_status`.

**Rationale:** Consistencia con lo que ya existe. Formato: `idx_{table}_{column}`.

### D4: Version de migracion — siguiente disponible por servicio

**Decision:** Usar la siguiente version Flyway disponible en cada servicio. Reservation tiene V3 (saga_state), asi que sera V4. Fleet y Payment tienen V2 (outbox), asi que sera V3.

## Risks / Trade-offs

**[Indices sin carga]** → En un POC sin volumen real, los indices no aportan rendimiento medible. Mitigacion: el valor es documentar buenas practicas, no el rendimiento en si.
