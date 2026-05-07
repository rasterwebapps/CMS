-- V108: Mark STAFF, FACULTY, STUDENT, and ALUMNI referral types as system-defined.
-- AGENT_REFERRAL was already marked system-defined in V54.
-- System-defined types cannot be deleted and their code / hasCommission flag cannot be changed.
UPDATE referral_types
SET is_system_defined = TRUE
WHERE code IN ('STAFF', 'FACULTY', 'STUDENT', 'ALUMNI');
