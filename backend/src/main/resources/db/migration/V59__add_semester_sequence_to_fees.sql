-- Allow same receipt number across multiple installments (one payment can span multiple semesters)
ALTER TABLE fee_installments DROP CONSTRAINT IF EXISTS fee_installments_receipt_number_key;
CREATE INDEX IF NOT EXISTS idx_fee_installments_receipt_number ON fee_installments(receipt_number);

-- Track which semester within a year (1 = first half, 2 = second half)
ALTER TABLE semester_fees ADD COLUMN IF NOT EXISTS semester_sequence INTEGER NOT NULL DEFAULT 1;
