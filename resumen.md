# Resumen: Change bruno-e2e-tests

## Objetivo

Crear una coleccion Bruno versionable en Git con requests para los 4 microservicios y una secuencia E2E que validara el happy path SAGA completo (PENDING → CONFIRMED) contra Docker Compose.

## Lo que se implemento

### Coleccion Bruno (20 ficheros `.bru`)

```
bruno/
├── bruno.json                          # Metadata de coleccion
├── environments/local.bru              # URLs: localhost:8181-8184
├── customer-service/                   # 5 requests (CRUD + suspend/activate)
├── fleet-service/                      # 5 requests (register + lifecycle)
├── reservation-service/                # 2 requests (create + track)
├── payment-service/                    # 3 requests (process + refund + get)
└── e2e/                                # 4 requests secuenciales
    ├── 01-create-customer.bru          # POST + extract customerId
    ├── 02-register-vehicle.bru         # POST + extract vehicleId
    ├── 03-create-reservation.bru       # POST + assert PENDING + extract trackingId
    └── 04-verify-confirmed.bru         # GET + assert CONFIRMED
```

- Variables de entorno (`{{customerUrl}}`, etc.) para no hardcodear puertos
- `{{$timestamp}}` en email y licensePlate para unicidad entre ejecuciones
- `bru.setVar()` en post-response scripts para encadenar IDs entre requests

## El bug: SAGA atascada en CUSTOMER_VALIDATED

### Sintoma

Al ejecutar `bru run --env local e2e`, los 3 primeros requests pasaban pero el paso 04 fallaba:

```
✓ res.status: eq 201
✓ res.body.data.status: eq PENDING
✕ res.body.data.status: eq CONFIRMED
  expected 'CUSTOMER_VALIDATED' to equal 'CONFIRMED'
```

La SAGA nunca progresaba mas alla de `CUSTOMER_VALIDATED`, sin importar cuanto tiempo se esperara (probamos 5s, 10s, 20s).

### Investigacion

1. **Logs del reservation-service** revelaron errores repetitivos en `PaymentCompletedResponseListener`:
   ```
   Caused by: java.lang.IllegalArgumentException: Invalid UUID string:
     at java.util.UUID.fromString(...)
     at PaymentCompletedResponseListener.handle(PaymentCompletedResponseListener.java:31)
   ```
   El UUID era un **string vacio**.

2. **Traza del flujo SAGA**:
   - PENDING → CustomerValidationStep → customer-service valida → CUSTOMER_VALIDATED ✅
   - CUSTOMER_VALIDATED → PaymentStep → payment-service procesa → publica `PaymentCompletedEvent` → reservation-service recibe... **y crashea** ❌
   - La excepcion causaba NACK del mensaje, que se reencolaba infinitamente

3. **Root cause: serializacion de value objects en domain events**

   Comparamos los eventos de los 3 servicios:

   | Servicio | Campo en evento | Tipo | JSON resultante |
   |----------|----------------|------|-----------------|
   | Customer | `reservationId` | `UUID` | `"reservationId": "abc-123"` ✅ |
   | Fleet | `reservationId` | `UUID` | `"reservationId": "abc-123"` ✅ |
   | **Payment** | `reservationId` | `ReservationId` | `"reservationId": {"value": "abc-123"}` ❌ |

   El `PaymentCompletedEvent` usaba el value object `ReservationId` (que envuelve un UUID) en lugar del `UUID` raw. Jackson lo serializaba como objeto anidado. El listener hacia:

   ```java
   UUID reservationId = UUID.fromString(json.get("reservationId").asText());
   ```

   `.asText()` sobre un ObjectNode devuelve `""` → `UUID.fromString("")` → `IllegalArgumentException`.

### Solucion

Cambiamos los 3 eventos de payment para usar `UUID` raw (consistente con customer/fleet):

**Ficheros modificados:**
- `payment-domain/.../event/PaymentCompletedEvent.java` — `ReservationId` → `UUID`
- `payment-domain/.../event/PaymentFailedEvent.java` — `ReservationId` → `UUID`
- `payment-domain/.../event/PaymentRefundedEvent.java` — `ReservationId` → `UUID`
- `payment-domain/.../aggregate/Payment.java` — `.reservationId` → `.reservationId.value()`
- `payment-domain/.../event/PaymentDomainEventsTest.java` — `RESERVATION_ID` cambiado a `UUID`

**Justificacion arquitectonica:** `reservationId` es un identificador foraneo en el bounded context de Payment. Los otros servicios (customer, fleet) ya usaban `UUID` raw para IDs cross-context. El value object `ReservationId` tiene sentido dentro del aggregate de Payment, pero en los domain events que cruzan fronteras de servicio, el UUID raw es lo correcto.

## Problema secundario: pre-request scripts en Bruno CLI

### Sintoma

El `script:pre-request` con `await new Promise(r => setTimeout(r, 5000))` no se ejecutaba en Bruno CLI. Probamos tambien un busy-wait sincrono — tampoco funciono. La duracion del request 04 era siempre ~30-40ms.

### Conclusion

Bruno CLI (v2.x) parece ignorar los bloques `script:pre-request` completamente. No afecto al resultado final porque la SAGA completa en ~2s (mucho antes de que el request 04 se ejecute, dado el tiempo de los 3 requests anteriores).

El script se dejo con `await` y un comentario explicativo — funciona en la UI de Bruno para uso manual.

## Resultado final

```
bru run --env local e2e

e2e\01-create-customer (201) - 638 ms       ✓ res.status: eq 201
e2e\02-register-vehicle (201) - 1005 ms     ✓ res.status: eq 201
e2e\03-create-reservation (201) - 752 ms    ✓ res.status: eq 201
                                             ✓ res.body.data.status: eq PENDING
e2e\04-verify-confirmed (200) - 41 ms       ✓ res.status: eq 200
                                             ✓ res.body.data.status: eq CONFIRMED

Status: ✓ PASS — 4/4 requests, 6/6 assertions
```

## Leccion aprendida

> **Los domain events que cruzan fronteras de bounded context deben usar tipos primitivos (UUID, String) para los IDs foraneos, no value objects del dominio local.** Los value objects son excelentes dentro del aggregate y del bounded context, pero Jackson los serializa como objetos anidados, rompiendo la deserializacion en el servicio consumidor que espera un string plano.
>
> Customer y Fleet ya seguian este patron. Payment no — y eso rompio silenciosamente la SAGA entera.
