CREATE TABLE vehicles (
    id                  UUID            PRIMARY KEY,
    license_plate       VARCHAR(255)    NOT NULL UNIQUE,
    make                VARCHAR(255)    NOT NULL,
    model               VARCHAR(255)    NOT NULL,
    year                INTEGER         NOT NULL,
    category            VARCHAR(50)     NOT NULL,
    daily_rate_amount   NUMERIC(10,2)   NOT NULL,
    daily_rate_currency VARCHAR(3)      NOT NULL,
    description         VARCHAR(500),
    status              VARCHAR(50)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL
);
