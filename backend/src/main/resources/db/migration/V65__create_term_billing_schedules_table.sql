CREATE TABLE term_billing_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    academic_year_id    BIGINT NOT NULL REFERENCES academic_years(id),
    term_type           VARCHAR(10) NOT NULL CHECK (term_type IN ('ODD', 'EVEN')),
    due_date            DATE NOT NULL,
    late_fee_type       VARCHAR(10) NOT NULL CHECK (late_fee_type IN ('FLAT', 'PER_DAY')),
    late_fee_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    grace_days          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (academic_year_id, term_type)
);
