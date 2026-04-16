-- Rename guideline_value to commission_amount
ALTER TABLE referral_types RENAME COLUMN guideline_value TO commission_amount;

-- Add has_commission flag
ALTER TABLE referral_types ADD COLUMN has_commission BOOLEAN NOT NULL DEFAULT FALSE;

-- Set has_commission = TRUE for AGENT_REFERRAL
UPDATE referral_types SET has_commission = TRUE WHERE code = 'AGENT_REFERRAL';
