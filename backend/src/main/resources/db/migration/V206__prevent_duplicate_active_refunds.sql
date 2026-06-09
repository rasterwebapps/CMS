-- Prevent concurrent duplicate active refund requests for the same original receipt.
-- Allows retry only after a request is REJECTED.
CREATE UNIQUE INDEX IF NOT EXISTS uq_fee_refunds_active_receipt
    ON fee_refunds (original_receipt_number)
    WHERE status <> 'REJECTED';

