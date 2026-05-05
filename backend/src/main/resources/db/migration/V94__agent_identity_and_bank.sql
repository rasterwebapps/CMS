-- V94: Add identity (PAN, Aadhaar) and bank-detail columns to agents.
ALTER TABLE agents ADD COLUMN IF NOT EXISTS pan_number          VARCHAR(20);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS aadhaar_number      VARCHAR(20);

ALTER TABLE agents ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(40);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bank_ifsc_code      VARCHAR(20);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bank_branch         VARCHAR(150);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bank_name           VARCHAR(150);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bank_account_holder VARCHAR(150);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bank_account_type   VARCHAR(20);
