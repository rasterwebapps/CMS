CREATE TABLE student_fee_allocations (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT                      NOT NULL REFERENCES students(id),
    program_id        BIGINT                      NOT NULL REFERENCES programs(id),
    total_fee         NUMERIC(12,2)               NOT NULL,
    discount_amount   NUMERIC(12,2),
    discount_reason   VARCHAR(500),
    agent_commission  NUMERIC(12,2),
    net_fee           NUMERIC(12,2)               NOT NULL,
    status            VARCHAR(50)                 NOT NULL,
    finalized_at      TIMESTAMP WITH TIME ZONE,
    finalized_by      VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE    NOT NULL
);
