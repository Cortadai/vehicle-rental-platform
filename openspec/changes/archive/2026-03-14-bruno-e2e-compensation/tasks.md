## 1. Reorganize e2e structure

- [x] 1.1 Move `bruno/e2e/*.bru` to `bruno/e2e/happy-path/`
- [x] 1.2 Verify `bru run --env local e2e/happy-path` passes (4/4 requests, 6/6 assertions)

## 2. Create compensation sequence

- [x] 2.1 Create `bruno/e2e/compensation/01-create-customer.bru` — POST + extract customerId + assert 201
- [x] 2.2 Create `bruno/e2e/compensation/02-register-vehicle.bru` — POST + extract vehicleId + assert 201
- [x] 2.3 Create `bruno/e2e/compensation/03-send-to-maintenance.bru` — POST to maintenance endpoint + assert 200
- [x] 2.4 Create `bruno/e2e/compensation/04-create-reservation.bru` — POST with chained IDs + extract trackingId + assert PENDING
- [x] 2.5 Create `bruno/e2e/compensation/05-verify-cancelled.bru` — sleep 8s + GET + assert CANCELLED + assert failureMessages not empty

## 3. Documentation

- [x] 3.1 Update `bruno/README.md` with new folder structure and compensation commands
- [x] 3.2 Update `CLAUDE.md` Bruno section with both run commands

## 4. Verification

- [x] 4.1 Start platform with `docker compose up -d` and verify services healthy
- [x] 4.2 Run `bru run --env local e2e/happy-path` — all 4 requests pass
- [x] 4.3 Run `bru run --env local e2e/compensation` — all 5 requests pass
