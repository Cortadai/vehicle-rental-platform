## 1. Collection Setup

- [x] 1.1 Create `bruno/bruno.json` with collection metadata
- [x] 1.2 Create `bruno/environments/local.bru` with `customerUrl`, `fleetUrl`, `reservationUrl`, `paymentUrl`

## 2. Customer Service Requests

- [x] 2.1 Create `bruno/customer-service/create-customer.bru` (POST)
- [x] 2.2 Create `bruno/customer-service/get-customer.bru` (GET)
- [x] 2.3 Create `bruno/customer-service/suspend-customer.bru` (POST)
- [x] 2.4 Create `bruno/customer-service/activate-customer.bru` (POST)
- [x] 2.5 Create `bruno/customer-service/delete-customer.bru` (DELETE)

## 3. Fleet Service Requests

- [x] 3.1 Create `bruno/fleet-service/register-vehicle.bru` (POST)
- [x] 3.2 Create `bruno/fleet-service/get-vehicle.bru` (GET)
- [x] 3.3 Create `bruno/fleet-service/send-to-maintenance.bru` (POST)
- [x] 3.4 Create `bruno/fleet-service/activate-vehicle.bru` (POST)
- [x] 3.5 Create `bruno/fleet-service/retire-vehicle.bru` (POST)

## 4. Reservation Service Requests

- [x] 4.1 Create `bruno/reservation-service/create-reservation.bru` (POST)
- [x] 4.2 Create `bruno/reservation-service/track-reservation.bru` (GET)

## 5. Payment Service Requests

- [x] 5.1 Create `bruno/payment-service/process-payment.bru` (POST)
- [x] 5.2 Create `bruno/payment-service/refund-payment.bru` (POST)
- [x] 5.3 Create `bruno/payment-service/get-payment.bru` (GET)

## 6. E2E SAGA Happy Path

- [x] 6.1 Create `bruno/e2e/01-create-customer.bru` — POST + extract customerId + assert 201
- [x] 6.2 Create `bruno/e2e/02-register-vehicle.bru` — POST + extract vehicleId + assert 201
- [x] 6.3 Create `bruno/e2e/03-create-reservation.bru` — POST with chained IDs + extract trackingId + assert PENDING
- [x] 6.4 Create `bruno/e2e/04-verify-confirmed.bru` — pre-request sleep 5s + GET + assert CONFIRMED

## 7. Verification

- [x] 7.1 Install Bruno CLI (`npm install -g @usebruno/cli`)
- [x] 7.2 Start platform with `docker compose up -d` and verify services healthy
- [x] 7.3 Run `bru run --env local` from `bruno/e2e/` — all 4 requests pass
- [x] 7.4 Update CLAUDE.md with Bruno run instructions
