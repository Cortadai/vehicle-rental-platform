## Context

La plataforma tiene 4 microservicios corriendo en Docker Compose (change #24). Las APIs REST estan probadas individualmente con 500+ tests Java (unitarios + IT con Testcontainers). Falta una verificacion E2E con los 4 servicios reales interactuando via RabbitMQ y el flujo SAGA completo.

Bruno es una herramienta de API testing que almacena las colecciones como ficheros `.bru` en el filesystem, lo que permite versionarlas en Git. Tiene una CLI (`@usebruno/cli`) para ejecucion desde terminal.

## Goals / Non-Goals

**Goals:**
- Coleccion Bruno versionable en Git con requests para los 4 servicios
- Secuencia E2E que valide el happy path SAGA: PENDING → CONFIRMED
- Ejecucion local via `bru run` contra Docker Compose
- Requests individuales por servicio para exploracion manual desde la UI de Bruno

**Non-Goals:**
- Tests de error cases, validaciones o compensation flows — cubiertos por los 492 tests Java
- Ejecucion en CI/CD — solo local por ahora
- Tests de rendimiento o carga
- Polling sofisticado o retry logic — sleep fijo de 5s es suficiente

## Decisions

### D1: Estructura de carpetas — por servicio + e2e separada

**Decision:** Coleccion con subcarpetas por servicio (requests individuales) + carpeta `e2e/` para el flujo automatizado.

**Alternativa descartada:** Todo en una sola carpeta plana. Mezclaria requests de exploracion con el flujo E2E.

**Rationale:** Los requests por servicio sirven para usar desde la UI de Bruno (exploracion manual). La carpeta `e2e/` es lo que ejecuta `bru run`. Separacion clara de propositos.

### D2: Prefijo numerico para orden de ejecucion

**Decision:** Los ficheros en `e2e/` usan prefijo numerico: `01-create-customer.bru`, `02-register-vehicle.bru`, etc.

**Rationale:** `bru run` ejecuta los ficheros en orden alfabetico dentro de la carpeta. El prefijo numerico garantiza el orden correcto sin depender de configuracion adicional.

### D3: Sleep fijo de 5s antes de verificacion

**Decision:** El paso 04 (`verify-confirmed.bru`) incluye un `await new Promise(r => setTimeout(r, 5000))` en su script `pre-request` para dar tiempo a que la SAGA complete.

**Alternativa descartada:** Script wrapper bash con polling, o request dummy intermedio para el delay.

**Rationale:** La SAGA tarda ~1-3s (outbox polling cada 100ms × 3 pasos). Un sleep de 5s da margen suficiente. Si en alguna maquina lenta falla, se sube a 8s. No merece la complejidad de un wrapper para un POC de aprendizaje.

### D4: Variables de coleccion para encadenar IDs

**Decision:** Usar `bru.setVar()` en scripts `post-response` para propagar `customerId`, `vehicleId` y `trackingId` entre requests de la secuencia E2E.

**Rationale:** Es el mecanismo nativo de Bruno para pasar datos entre requests. Las variables se resuelven con `{{variableName}}` en el body de los requests siguientes.

### D5: Environment con puertos por servicio

**Decision:** Un environment `local` con variables `customerUrl`, `fleetUrl`, `reservationUrl`, `paymentUrl` apuntando a `http://localhost:<port>`.

**Alternativa descartada:** Una sola variable `baseUrl` con el puerto como parte del path. No funciona porque cada servicio esta en un puerto diferente.

**Rationale:** Cada servicio corre en un puerto distinto (8181-8184). Variables separadas permiten construir las URLs sin hardcodear puertos en cada request.

### D6: Datos de test con unicidad via timestamp

**Decision:** Los campos con constraint de unicidad (email del customer, licensePlate del vehicle) SHALL usar `{{$timestamp}}` de Bruno para generar valores unicos en cada ejecucion. El resto de campos usan valores fijos legibles.

**Alternativa descartada:** Datos 100% fijos + `docker compose down -v` entre ejecuciones. Obliga a resetear la BD entera para poder re-ejecutar.

**Rationale:** Permite ejecutar `bru run` multiples veces sin conflictos de unicidad. Ejemplo: `e2e-{{$timestamp}}@test.com` y `TEST-{{$timestamp}}`. Los campos sin constraint (nombre, ciudad, etc.) se mantienen fijos para legibilidad.

## Risks / Trade-offs

**[Sleep fragil]** → En maquinas lentas el sleep de 5s podria no ser suficiente. Mitigacion: subir a 8-10s si falla. Aceptable para POC local.

**[Datos duplicados]** → Mitigado con `{{$timestamp}}` en email y licensePlate. Se puede ejecutar `bru run` multiples veces sin reset de BD.

**[Bruno CLI compatibility]** → Los scripts `pre-request` con `await` podrian no funcionar en todas las versiones de Bruno CLI. Mitigacion: verificar durante implementacion, alternativa es `setTimeout` con callback.
