## Why

La plataforma tiene 500+ tests automatizados (unitarios + integracion) pero ninguna verificacion end-to-end con los 4 servicios corriendo simultaneamente en Docker Compose. Tras completar el change `docker-compose-services` (#24), la plataforma levanta con un solo `docker compose up -d`. Ahora necesitamos una coleccion Bruno que valide el flujo SAGA completo contra los servicios reales, y que sirva como herramienta de exploracion manual de las APIs.

## What Changes

- Nueva carpeta `bruno/` en la raiz del repositorio con coleccion Bruno versionable en Git
- Environment `local` configurado para los 4 servicios (puertos 8181-8184)
- Requests individuales por servicio para exploracion manual desde la UI de Bruno
- Carpeta `e2e/` con secuencia ordenada que prueba el happy path SAGA completo:
  1. Crear customer → extraer `customerId`
  2. Registrar vehicle → extraer `vehicleId`
  3. Crear reservation → extraer `trackingId`, verificar `PENDING`
  4. Esperar 5s + verificar que la reserva llega a `CONFIRMED`
- Variables de coleccion para encadenar IDs entre requests
- Ejecucion via `bru run` desde la CLI

## Capabilities

### New Capabilities
- `bruno-collection`: Coleccion Bruno con environments, requests por servicio y estructura de carpetas versionable
- `bruno-e2e-saga`: Secuencia E2E que valida el happy path SAGA completo (PENDING → CONFIRMED)

### Modified Capabilities
(ninguna — este change no modifica comportamiento existente)

## Impact

- **Ficheros nuevos**: carpeta `bruno/` con ~20 ficheros `.bru` + `bruno.json` + environment
- **Dependencias**: Bruno CLI (`npm install -g @usebruno/cli`) necesario para ejecutar `bru run`
- **Sin cambios en codigo Java**: las APIs no se modifican
- **Sin impacto en build**: `bruno/` no participa en `mvn verify`
- **Prerequisito**: `docker compose up -d` debe estar corriendo con los 4 servicios healthy
