CREATE TABLE saga_state (
    saga_id         UUID            PRIMARY KEY,
    saga_type       VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    current_step    INT             NOT NULL DEFAULT 0,
    total_steps     INT             NOT NULL,
    payload         TEXT            NOT NULL,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_saga_state_status ON saga_state(status);
