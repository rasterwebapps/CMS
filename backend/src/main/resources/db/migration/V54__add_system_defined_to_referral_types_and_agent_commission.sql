-- Add is_system_defined flag to referral_types
ALTER TABLE referral_types ADD COLUMN is_system_defined BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark the AGENT_REFERRAL referral type as system-defined
UPDATE referral_types SET is_system_defined = TRUE WHERE code = 'AGENT_REFERRAL';

-- Add commission_amount to agents (nullable, agent-specific override)
ALTER TABLE agents ADD COLUMN commission_amount NUMERIC(10, 2);
