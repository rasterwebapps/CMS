-- ==========================================================================
-- V122: Unified Payment Receipts Table + Global Sequential Numbering
-- ==========================================================================
-- Covers both student fee installments and enquiry payments in one view.
-- Uses a year-scoped sequence so receipt numbers are sequential and predictable.
-- ==========================================================================

-- 1. Global receipt number sequence (year-scoped, resets each year)
CREATE TABLE receipt_number_sequence (
    year     INTEGER PRIMARY KEY,
    last_seq INTEGER NOT NULL DEFAULT 0
);

-- Seed the current year row
INSERT INTO receipt_number_sequence (year, last_seq)
VALUES (EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 0)
ON CONFLICT (year) DO NOTHING;

-- 2. Unified payment receipts
CREATE TABLE payment_receipts (
    id                    BIGSERIAL PRIMARY KEY,
    receipt_number        VARCHAR(50)    NOT NULL UNIQUE,
    payer_type            VARCHAR(10)    NOT NULL CHECK (payer_type IN ('STUDENT', 'ENQUIRY')),
    payer_id              BIGINT         NOT NULL,
    payer_name            VARCHAR(255)   NOT NULL,
    payer_identifier      VARCHAR(50),
    program_name          VARCHAR(255),
    amount_paid           NUMERIC(12, 2) NOT NULL,
    payment_date          DATE           NOT NULL,
    payment_mode          VARCHAR(30)    NOT NULL,
    transaction_reference VARCHAR(255),
    remarks               TEXT,
    installments_covered  TEXT,
    collected_by          VARCHAR(100),
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_receipts_payer  ON payment_receipts (payer_type, payer_id);
CREATE INDEX idx_payment_receipts_date   ON payment_receipts (payment_date DESC);
CREATE INDEX idx_payment_receipts_rcptno ON payment_receipts (receipt_number);

-- 3. Backfill existing student fee receipts (grouped by receipt_number)
INSERT INTO payment_receipts (
    receipt_number, payer_type, payer_id, payer_name, payer_identifier,
    program_name, amount_paid, payment_date, payment_mode,
    transaction_reference, remarks, installments_covered, collected_by, created_at
)
SELECT
    fi.receipt_number,
    'STUDENT',
    s.id,
    s.first_name || ' ' || s.last_name,
    s.roll_number,
    NULL,
    SUM(fi.amount_paid),
    fi.payment_date,
    fi.payment_mode::VARCHAR,
    MAX(fi.transaction_reference),
    MAX(fi.remarks),
    string_agg(sf.installment_label, ', ' ORDER BY sf.year_number, sf.sequence),
    NULL,
    MIN(fi.created_at)
FROM fee_installments fi
JOIN students s       ON s.id = fi.student_id
JOIN installment_fees sf ON sf.id = fi.semester_fee_id
GROUP BY fi.receipt_number, s.id, s.first_name, s.last_name, s.roll_number,
         fi.payment_date, fi.payment_mode
ON CONFLICT (receipt_number) DO NOTHING;

-- 4. Backfill existing enquiry payments
INSERT INTO payment_receipts (
    receipt_number, payer_type, payer_id, payer_name, payer_identifier,
    program_name, amount_paid, payment_date, payment_mode,
    transaction_reference, remarks, installments_covered, collected_by, created_at
)
SELECT
    ep.receipt_number,
    'ENQUIRY',
    e.id,
    e.name,
    NULL,
    p.name,
    ep.amount_paid,
    ep.payment_date,
    ep.payment_mode::VARCHAR,
    ep.transaction_reference,
    ep.remarks,
    'Pre-enrollment Fee',
    ep.collected_by,
    ep.created_at
FROM enquiry_payments ep
JOIN enquiries e ON e.id = ep.enquiry_id
LEFT JOIN programs p ON p.id = e.program_id
ON CONFLICT (receipt_number) DO NOTHING;

-- 5. Add RECEIPT_VIEW permission
INSERT INTO permissions (code, display_name, category, created_at)
VALUES ('RECEIPT_VIEW', 'View Receipts', 'FINANCE', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- 6. Grant RECEIPT_VIEW to finance roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE p.code = 'RECEIPT_VIEW'
  AND r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN', 'CASHIER')
ON CONFLICT DO NOTHING;

