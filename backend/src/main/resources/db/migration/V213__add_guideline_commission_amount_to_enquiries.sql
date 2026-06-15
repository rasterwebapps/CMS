-- V213: Track the guideline-resolved commission snapshot alongside the user-finalised amount.
-- guidelineCommissionAmount stores what the AgentCommissionGuideline suggested at create/update time
-- so any manual override is auditable by comparing it to commission_amount.
ALTER TABLE enquiries ADD COLUMN guideline_commission_amount NUMERIC(12, 2);
