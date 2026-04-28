CREATE TABLE term_fee_payments (
    id                  BIGSERIAL PRIMARY KEY,
    fee_demand_id       BIGINT NOT NULL REFERENCES fee_demands(id),
    payment_date        DATE NOT NULL,
    amount_paid         NUMERIC(12, 2) NOT NULL,
    late_fee_applied    NUMERIC(10, 2) NOT NULL DEFAULT 0,
    payment_mode        VARCHAR(50) NOT NULL,
    receipt_number      VARCHAR(50) NOT NULL UNIQUE,
    remarks             VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tfp_fee_demand ON term_fee_payments(fee_demand_id);
CREATE INDEX idx_tfp_payment_date ON term_fee_payments(payment_date);
