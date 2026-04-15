CREATE TABLE fee_structure_year_amounts (
    id                BIGSERIAL PRIMARY KEY,
    fee_structure_id  BIGINT                      NOT NULL REFERENCES fee_structures(id) ON DELETE CASCADE,
    year_number       INTEGER                     NOT NULL,
    year_label        VARCHAR(100)                NOT NULL,
    amount            NUMERIC(12,2)               NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fee_structure_year_amounts_fee_structure ON fee_structure_year_amounts(fee_structure_id);
