ALTER TABLE student_fee_allocations
    ADD COLUMN scholarship_application_id BIGINT REFERENCES student_scholarships(id),
    ADD COLUMN scholarship_discount_amount NUMERIC(12,2),
    ADD COLUMN scholarship_discount_reason VARCHAR(500);

CREATE INDEX idx_student_fee_allocations_scholarship_application
    ON student_fee_allocations(scholarship_application_id);

