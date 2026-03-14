## Why

El happy path SAGA (PENDING → CONFIRMED) esta validado con Bruno E2E (change #25). Falta validar el compensation flow — el camino inverso donde fleet rechaza el vehiculo y la SAGA ejecuta la cascade de compensacion (refund payment → cancel reservation). Es el unico escenario que ejercita compensacion real (PaymentStep.rollback). Ademas, reorganizar la estructura de `e2e/` para separar happy path y compensation en subcarpetas.

## What Changes

- Reorganizar `bruno/e2e/` en subcarpetas: `e2e/happy-path/` y `e2e/compensation/`
- Mover los 4 ficheros existentes a `e2e/happy-path/`
- Crear 5 ficheros en `e2e/compensation/`:
  1. Crear customer (ACTIVE)
  2. Registrar vehicle (ACTIVE)
  3. Enviar vehicle a mantenimiento (status → MAINTENANCE)
  4. Crear reserva con ese vehicle → PENDING
  5. Esperar + verificar CANCELLED con failureMessages
- Actualizar CLAUDE.md y bruno/README.md con los nuevos comandos

## Capabilities

### New Capabilities
- `bruno-e2e-compensation`: Secuencia E2E que valida el compensation flow SAGA (fleet rejection → payment refund → CANCELLED)

### Modified Capabilities
- `bruno-e2e-saga`: Reorganizacion de carpetas (e2e/ → e2e/happy-path/) — sin cambio de comportamiento

## Impact

- **Ficheros nuevos**: 5 ficheros `.bru` en `e2e/compensation/`
- **Ficheros movidos**: 4 ficheros `.bru` de `e2e/` a `e2e/happy-path/`
- **Sin cambios en codigo Java**: las APIs y la SAGA no se modifican
- **Prerequisito**: `docker compose up -d` con los 4 servicios
