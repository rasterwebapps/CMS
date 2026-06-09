-- V210: Drop the legacy fee_payments table.
-- This table was superseded by the unified payment_receipts system (V122).
-- All data was cleared in V167. No application code writes to it; removing
-- the table and all associated application layer code (Gap 5 cleanup).

DROP TABLE IF EXISTS fee_payments;
