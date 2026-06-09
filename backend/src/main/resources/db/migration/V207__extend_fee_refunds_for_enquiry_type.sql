-- V207: Extend fee_refunds to support ENQUIRY entity type alongside STUDENT.
-- Also adds soft-flag reversal columns to enquiry_payments (mirrors fee_installments.refunded_at pattern).

-- 1. Extend fee_refunds
ALTER TABLE fee_refunds
    ADD COLUMN entity_type  VARCHAR(10) NOT NULL DEFAULT 'STUDENT',
    ADD COLUMN enquiry_id   BIGINT,
    ALTER COLUMN student_id   DROP NOT NULL,
    ALTER COLUMN student_name DROP NOT NULL;

-- 2. Add soft-flag reversal tracking to enquiry_payments
ALTER TABLE enquiry_payments
    ADD COLUMN refunded_at    TIMESTAMP,
    ADD COLUMN refund_number  VARCHAR(50);
