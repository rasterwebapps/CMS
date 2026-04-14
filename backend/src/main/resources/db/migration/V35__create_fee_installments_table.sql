CREATE TABLE fee_installments (
    id                    BIGSERIAL PRIMARY KEY,
    semester_fee_id       BIGINT                      NOT NULL REFERENCES semester_fees(id),
    student_id            BIGINT                      NOT NULL REFERENCES students(id),
    amount_paid           NUMERIC(12,2)               NOT NULL,
    payment_date          DATE                        NOT NULL,
    payment_mode          VARCHAR(50)                 NOT NULL,
    receipt_number        VARCHAR(100)                NOT NULL UNIQUE,
    transaction_reference VARCHAR(255),
    remarks               VARCHAR(500),
    created_at            TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE    NOT NULL
);
