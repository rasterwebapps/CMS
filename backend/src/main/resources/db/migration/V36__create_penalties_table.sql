CREATE TABLE penalties (
    id                  BIGSERIAL PRIMARY KEY,
    semester_fee_id     BIGINT                      NOT NULL REFERENCES semester_fees(id),
    student_id          BIGINT                      NOT NULL REFERENCES students(id),
    daily_rate          NUMERIC(10,2)               NOT NULL,
    penalty_start_date  DATE                        NOT NULL,
    penalty_end_date    DATE,
    total_penalty       NUMERIC(12,2)               NOT NULL,
    is_paid             BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE    NOT NULL
);
