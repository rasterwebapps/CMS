CREATE TABLE enquiry_payments (
    id                    BIGSERIAL PRIMARY KEY,
    enquiry_id            BIGINT NOT NULL REFERENCES enquiries(id),
    amount_paid           NUMERIC(12,2) NOT NULL,
    payment_date          DATE NOT NULL,
    payment_mode          VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(255),
    remarks               TEXT,
    receipt_number        VARCHAR(255) NOT NULL UNIQUE,
    collected_by          VARCHAR(255),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
);
