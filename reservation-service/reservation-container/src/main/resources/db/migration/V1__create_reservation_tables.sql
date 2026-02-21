CREATE TABLE reservations (
    id                   UUID            PRIMARY KEY,
    tracking_id          UUID            NOT NULL UNIQUE,
    customer_id          UUID            NOT NULL,
    pickup_address       VARCHAR(255)    NOT NULL,
    pickup_city          VARCHAR(255)    NOT NULL,
    return_address       VARCHAR(255)    NOT NULL,
    return_city          VARCHAR(255)    NOT NULL,
    pickup_date          DATE            NOT NULL,
    return_date          DATE            NOT NULL,
    total_price_amount   NUMERIC         NOT NULL,
    total_price_currency VARCHAR(10)     NOT NULL,
    status               VARCHAR(50)     NOT NULL,
    failure_messages     TEXT,
    created_at           TIMESTAMPTZ     NOT NULL,
    updated_at           TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_reservations_tracking_id ON reservations (tracking_id);

CREATE TABLE reservation_items (
    id                  UUID            PRIMARY KEY,
    reservation_id      UUID            NOT NULL REFERENCES reservations(id),
    vehicle_id          UUID            NOT NULL,
    daily_rate_amount   NUMERIC         NOT NULL,
    daily_rate_currency VARCHAR(10)     NOT NULL,
    days                INTEGER         NOT NULL,
    subtotal_amount     NUMERIC         NOT NULL,
    subtotal_currency   VARCHAR(10)     NOT NULL
);
