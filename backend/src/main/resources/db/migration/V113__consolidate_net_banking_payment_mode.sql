-- V113: Consolidate NET_BANKING into BANK_TRANSFER
-- NET_BANKING and BANK_TRANSFER are equivalent — both represent online bank-initiated transfers.
-- This migration unifies them to BANK_TRANSFER for clarity and removes the duplicate.

UPDATE fee_payments
SET payment_mode = 'BANK_TRANSFER'
WHERE payment_mode = 'NET_BANKING';

UPDATE enquiry_payments
SET payment_mode = 'BANK_TRANSFER'
WHERE payment_mode = 'NET_BANKING';

UPDATE term_fee_payments
SET payment_mode = 'BANK_TRANSFER'
WHERE payment_mode = 'NET_BANKING';

-- agent commission payouts
UPDATE agent_commission_payouts
SET payment_mode = 'BANK_TRANSFER'
WHERE payment_mode = 'NET_BANKING';

-- fee_installments table
UPDATE fee_installments
SET payment_mode = 'BANK_TRANSFER'
WHERE payment_mode = 'NET_BANKING';

