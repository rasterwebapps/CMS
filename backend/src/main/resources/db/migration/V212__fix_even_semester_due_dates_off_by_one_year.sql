-- V212: Fix EVEN semester due dates that are one year too early.
--
-- Root cause: the EVEN term billing schedule for one or more academic years
-- had its due_date set to a date in the AY start year (e.g. 2023-05-31 for
-- AY 2023-24) instead of the AY end year (2024-05-31). The fallback in
-- V174/V176 reproduced this error when computing installment_fees.due_date
-- for students whose target academic year has no billing schedule yet.
--
-- Step 1: Correct term_billing_schedules.due_date for any EVEN row where
--         the due_date year equals the AY start year.
--         (EVEN term spans Dec–May, so the due date must fall in start_year + 1.)
--
-- Step 2: Fix installment_fees.due_date for every EVEN (sequence = 2) row
--         where the stored year equals joiningStartYear + year_number − 1
--         instead of the correct joiningStartYear + year_number.
--         These rows are detected by joining back to the student's cohort and
--         admission academic year; only cohort-linked students are affected
--         (students without a cohort have NULL cohort_id and are skipped by the JOIN).

-- ── Step 1: fix term_billing_schedules source data ───────────────────────────
UPDATE term_billing_schedules tbs
SET due_date = (tbs.due_date + INTERVAL '1 year')::DATE
WHERE tbs.term_type = 'EVEN'
  AND EXTRACT(YEAR FROM tbs.due_date)::INTEGER = (
      SELECT EXTRACT(YEAR FROM ay.start_date)::INTEGER
      FROM   academic_years ay
      WHERE  ay.id = tbs.academic_year_id
  );

-- ── Step 2: fix installment_fees for affected students ───────────────────────
UPDATE installment_fees sf
SET    due_date = (sf.due_date + INTERVAL '1 year')::DATE
FROM   student_fee_allocations sfa
JOIN   students               s        ON s.id          = sfa.student_id
JOIN   cohorts                c        ON c.id          = s.cohort_id
JOIN   academic_years         ay_admit ON ay_admit.id   = c.admission_academic_year_id
WHERE  sfa.id       = sf.allocation_id
  AND  sf.sequence  = 2                        -- EVEN installments only
  AND  EXTRACT(YEAR FROM sf.due_date)::INTEGER
       = EXTRACT(YEAR FROM ay_admit.start_date)::INTEGER + sf.year_number - 1;
