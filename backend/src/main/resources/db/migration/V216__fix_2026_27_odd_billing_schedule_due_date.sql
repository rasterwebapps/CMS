-- V216: Fix AY 2026-2027 ODD term billing schedule due date.
--
-- All other academic years follow the pattern: ODD due_date = start_year-12-31.
-- AY 2026-2027 ODD was incorrectly set to 2025-12-31 (copied from AY 2025-2026)
-- instead of 2026-12-31. New fee allocations would have inherited this wrong date.
-- Existing installment_fees for this slot used the shiftDueYear fallback and
-- already carry 2026-11-30, so no installment rows need correction.

UPDATE term_billing_schedules tbs
SET    due_date = '2026-12-31'
FROM   academic_years ay
WHERE  ay.id        = tbs.academic_year_id
  AND  ay.name      = '2026-2027'
  AND  tbs.term_type = 'ODD'
  AND  tbs.due_date  = '2025-12-31';
