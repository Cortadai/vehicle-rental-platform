# Bruno API Collection

Colección de requests HTTP para la plataforma **Vehicle Rental Platform**, organizada con [Bruno](https://www.usebruno.com/) — una herramienta de API testing que almacena las colecciones como ficheros de texto plano (`.bru`) versionables en Git.

---

## Prerequisitos

**1. Plataforma levantada**

```bash
# Desde la raíz del proyecto
docker compose up -d
```

Esto levanta PostgreSQL, RabbitMQ y los 4 microservicios. Espera unos segundos hasta que todos estén healthy.

**2. Bruno CLI instalado** (para ejecución desde terminal)

```bash
npm install -g @usebruno/cli
bru --version
```

**3. Bruno Desktop instalado** (para exploración manual con UI)

Descarga desde [usebruno.com](https://www.usebruno.com/downloads) y abre la carpeta `bruno/` como colección.

---

## Estructura de carpetas

```
bruno/
├── bruno.json                    # Metadata de la colección (requerido por Bruno)
├── environments/
│   └── local.bru                 # URLs de los 4 servicios (localhost:8181-8184)
├── customer-service/             # Requests de exploración — Customer Service
│   ├── create-customer.bru
│   ├── get-customer.bru
│   ├── suspend-customer.bru
│   ├── activate-customer.bru
│   └── delete-customer.bru
├── fleet-service/                # Requests de exploración — Fleet Service
│   ├── register-vehicle.bru
│   ├── get-vehicle.bru
│   ├── send-to-maintenance.bru
│   ├── activate-vehicle.bru
│   └── retire-vehicle.bru
├── reservation-service/          # Requests de exploración — Reservation Service
│   ├── create-reservation.bru
│   └── track-reservation.bru
├── payment-service/              # Requests de exploración — Payment Service
│   ├── process-payment.bru
│   ├── refund-payment.bru
│   └── get-payment.bru
└── e2e/                          # Flujo E2E automatizado — Happy Path SAGA
    ├── 01-create-customer.bru
    ├── 02-register-vehicle.bru
    ├── 03-create-reservation.bru
    └── 04-verify-confirmed.bru
```

### Carpetas por servicio (`customer-service/`, `fleet-service/`, etc.)

Requests individuales pensados para **exploración manual desde la UI de Bruno**. Útiles para probar endpoints concretos durante el desarrollo. No tienen aserciones ni scripts — los IDs (`{{customerId}}`, `{{vehicleId}}`, etc.) hay que rellenarlos a mano en la UI.

### Carpeta `e2e/`

Secuencia automatizada que valida el **happy path SAGA completo**. Los ficheros están numerados para garantizar el orden de ejecución. Tienen aserciones y scripts que encadenan IDs entre requests automáticamente.

---

## Ejecutar el E2E (CLI)

```bash
# Desde la carpeta bruno/
cd bruno
bru run --env local e2e
```

Salida esperada:

```
e2e\01-create-customer (201) - 283 ms
  ✓ res.status: eq 201

e2e\02-register-vehicle (201) - 276 ms
  ✓ res.status: eq 201

e2e\03-create-reservation (201) - 278 ms
  ✓ res.status: eq 201
  ✓ res.body.data.status: eq PENDING

e2e\04-verify-confirmed (200) - 272 ms
  ✓ res.status: eq 200
  ✓ res.body.data.status: eq CONFIRMED

Requests: 4 (4 Passed) | Assertions: 6/6 | Status: ✓ PASS
```

### Qué valida el E2E

| Paso | Request | Qué verifica |
|------|---------|--------------|
| 01 | `POST /api/v1/customers` | Customer creado → extrae `customerId` |
| 02 | `POST /api/v1/vehicles` | Vehicle registrado → extrae `vehicleId` |
| 03 | `POST /api/v1/reservations` | Reserva creada en estado `PENDING` → extrae `trackingId` |
| 04 | `GET /api/v1/reservations/{trackingId}` | SAGA completada → estado `CONFIRMED` |

El paso 04 espera a que la SAGA complete el flujo `PENDING → CUSTOMER_VALIDATED → PAID → CONFIRMED` a través de RabbitMQ.

---

## Ejecuciones repetidas y datos de prueba

Los ficheros E2E usan `{{$timestamp}}` para garantizar unicidad en cada ejecución:

```
"email": "e2e-{{$timestamp}}@test.com"
"licensePlate": "TEST-{{$timestamp}}"
```

Esto permite lanzar `bru run` múltiples veces sin conflictos de datos duplicados. Si quieres partir de una base de datos limpia:

```bash
docker compose down -v   # elimina los volúmenes (borra todos los datos)
docker compose up -d     # recrea todo desde cero (Flyway re-ejecuta las migraciones)
```

---

## Environment `local`

Definido en `environments/local.bru`. Apunta a los 4 servicios en localhost:

| Variable | URL |
|----------|-----|
| `customerUrl` | `http://localhost:8181` |
| `fleetUrl` | `http://localhost:8182` |
| `reservationUrl` | `http://localhost:8183` |
| `paymentUrl` | `http://localhost:8184` |

---

## Formato `.bru`

Los ficheros `.bru` son texto plano con bloques semánticos. Ejemplo de un request con aserción y script:

```
meta {
  name: 01 Create Customer
  type: http
  seq: 1
}

post {
  url: {{customerUrl}}/api/v1/customers
  body: json
  auth: none
}

body:json {
  {
    "firstName": "E2E",
    "email": "e2e-{{$timestamp}}@test.com"
  }
}

assert {
  res.status: eq 201          ← aserción sobre el status HTTP
}

script:post-response {
  bru.setVar("customerId", res.body.data.customerId);  ← extrae el ID para el siguiente request
}
```

Todas las respuestas de la plataforma siguen el wrapper `ApiResponse`:

```json
{
  "data": { ... },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

Por eso los scripts acceden a `res.body.data.customerId` y no a `res.body.customerId`.
