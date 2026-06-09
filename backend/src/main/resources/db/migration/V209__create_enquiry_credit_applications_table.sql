-- V209: Record enquiry pre-payment credit applications to student semester fees.
-- Each row is one credit application — how much of an enquiry payment was applied
-- against a specific semester fee when the student's first cash payment was collected.

CREATE TABLE enquiry_credit_applications (
    id              BIGSERIAL        PRIMARY KEY,
    enquiry_id      BIGINT           NOT NULL REFERENCES enquiries(id),
    student_id      BIGINT           NOT NULL REFERENCES students(id),
    semester_fee_id BIGINT           NOT NULL REFERENCES installment_fees(id),
    amount_applied  NUMERIC(12, 2)   NOT NULL,
    receipt_number  VARCHAR(50)      NOT NULL,
    applied_at      TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_eca_enquiry  ON enquiry_credit_applications(enquiry_id);
CREATE INDEX idx_eca_student  ON enquiry_credit_applications(student_id);
