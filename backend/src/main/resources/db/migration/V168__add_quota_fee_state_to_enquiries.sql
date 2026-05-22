-- BR-30: Add admission quota and fee state to enquiries
ALTER TABLE enquiries
    ADD COLUMN admission_quota VARCHAR(20),
    ADD COLUMN fee_state_id    BIGINT REFERENCES fee_states(id);

-- Existing enquiries left NULL (new enquiries only per BR-30 decision)
