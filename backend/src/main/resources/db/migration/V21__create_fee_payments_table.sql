CREATE TABLE fee_payments (
    id                    BIGSERIAL PRIMARY KEY,
    student_id            BIGINT                      NOT NULL REFERENCES students(id),
    fee_structure_id      BIGINT                      NOT NULL REFERENCES fee_structures(id),
    receipt_number        VARCHAR(255)                UNIQUE,
    amount_paid           NUMERIC(10,2)               NOT NULL,
    payment_date          DATE                        NOT NULL,
    payment_mode          VARCHAR(255)                NOT NULL,
    status                VARCHAR(255)                NOT NULL,
    transaction_reference VARCHAR(255),
    remarks               VARCHAR(255),
    created_at            TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE    NOT NULL
);
