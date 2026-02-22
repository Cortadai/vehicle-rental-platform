CREATE TABLE payments (
    id                  UUID            PRIMARY KEY,
    reservation_id      UUID            NOT NULL UNIQUE,
    customer_id         UUID            NOT NULL,
    amount              NUMERIC(10,2)   NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    failure_messages    TEXT,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);
