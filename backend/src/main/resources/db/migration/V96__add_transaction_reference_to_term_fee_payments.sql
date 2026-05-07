-- Add transaction_reference column to term_fee_payments table
-- Required for UPI, Bank Transfer, and Cheque payment modes

ALTER TABLE term_fee_payments
ADD COLUMN transaction_reference VARCHAR(255);

COMMENT ON COLUMN term_fee_payments.transaction_reference IS 'Transaction reference number (UTR, Cheque No., etc.) - mandatory for UPI, Bank Transfer, and Cheque';

