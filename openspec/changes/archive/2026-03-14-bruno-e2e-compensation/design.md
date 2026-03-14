## Context

La SAGA tiene 3 escenarios de fallo: customer rejection (step 0), payment failure (step 1), fleet rejection (step 2). Solo el escenario de fleet rejection ejerce compensacion real (PaymentStep.rollback → payment.refund.command). Los otros dos fallan sin compensacion.

## Goals / Non-Goals

**Goals:**
- E2E que valide la cascade completa: fleet rechaza → payment refund → reservation CANCELLED
- Assertions en status (CANCELLED) y failureMessages (motivo del rechazo)
- Reorganizar e2e/ en subcarpetas para happy path y compensation

**Non-Goals:**
- Escenarios A (customer rejection) y B (payment failure) — no ejercitan compensacion
- Polling sofisticado o retry — sleep fijo como en happy path

## Decisions

### D1: Forzar rechazo via send-to-maintenance

**Decision:** Crear un vehicle ACTIVE, luego enviarlo a MAINTENANCE con `POST /vehicles/{id}/maintenance` antes de crear la reserva. Fleet rechaza porque `status != ACTIVE`.

**Alternativa descartada:** Usar un vehicleId inexistente. Funciona pero es menos realista — un vehicle en mantenimiento es un caso de negocio real.

### D2: Subcarpetas en e2e/

**Decision:** `e2e/happy-path/` y `e2e/compensation/` en vez de carpetas paralelas como `e2e-happy/` y `e2e-compensation/`.

**Rationale:** `bru run` puede apuntar a subcarpetas: `bru run --env local e2e/happy-path`. Agrupa ambos flujos bajo el concepto E2E.

### D3: Assertions en failureMessages

**Decision:** Verificar que `res.body.data.failureMessages` contiene al menos un mensaje. No verificar el texto exacto para evitar fragilidad.

**Rationale:** El texto del error ("Vehicle is not available, current status: MAINTENANCE") es un detalle de implementacion. Verificar que hay al menos un mensaje confirma que la compensacion propago el motivo.

### D4: Sleep de 8 segundos antes de verificacion

**Decision:** El compensation flow tiene mas pasos asincronos que el happy path (fleet reject → refund command → refund response → cancel). Usar 8s de sleep como margen.

**Rationale:** El happy path completa en ~2s. La compensation cascade tiene 2 roundtrips extra via RabbitMQ. 8s da margen suficiente.

## Risks / Trade-offs

**[Sleep fragil]** → Si 8s no basta, subir a 12s. Mismo trade-off que el happy path.
