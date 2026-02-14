CREATE TABLE customers (
    id          UUID            PRIMARY KEY,
    first_name  VARCHAR(255)    NOT NULL,
    last_name   VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    phone       VARCHAR(50),
    status      VARCHAR(50)     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL
);
