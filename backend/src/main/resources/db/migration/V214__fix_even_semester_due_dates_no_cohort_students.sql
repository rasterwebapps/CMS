-- V214: Fix EVEN semester due dates for students without a cohort (cohort_id IS NULL).
--
-- V212 fixed the same off-by-one-year bug for cohort-linked students using an
-- INNER JOIN to cohorts, which silently skipped students whose cohort_id is NULL.
-- This migration applies the identical correction for those students, using
-- students.admission_date to derive the expected admission year.
--
-- Affected groups (all sharing the AY 2026-27 EVEN slot with due_date 2026-05-31):
--   2023 batch — year_number=4 (8th sem): 44 students
--   2024 batch — year_number=3 (6th sem): 53 students
--   2025 batch — year_number=2 (4th sem): 59 students

UPDATE installment_fees sf
SET    due_date = (sf.due_date + INTERVAL '1 year')::DATE
FROM   student_fee_allocations sfa
JOIN   students               s  ON s.id  = sfa.student_id
WHERE  sfa.id       = sf.allocation_id
  AND  s.cohort_id IS NULL
  AND  sf.sequence  = 2
  AND  EXTRACT(YEAR FROM sf.due_date)::INTEGER
       = EXTRACT(YEAR FROM s.admission_date)::INTEGER + sf.year_number - 1;
