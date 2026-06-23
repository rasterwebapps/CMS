-- V37_1 and V45 originally seeded this row as 'Agent Referral'. A later commit
-- edited those already-applied migration files to seed 'Agent' instead, which
-- only takes effect on fresh databases — environments that had already run
-- V37_1/V45 keep the stale name forever. This forward migration corrects it
-- everywhere without touching the historical migration files.
UPDATE referral_types
SET name = 'Agent', updated_at = CURRENT_TIMESTAMP
WHERE code = 'AGENT_REFERRAL' AND name = 'Agent Referral';
