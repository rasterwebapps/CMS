-- V177: Fix installment due dates for students without a cohort.
-- V174 and V176 both JOIN on cohorts, so they skip students where cohort_id IS NULL.
-- For those students, Years 2-4 got due_date = creation date (wrong).
-- Fix: shift Year-1's correct due_date forward by (year_number - 1) years.
-- Safety guard: only update rows where Year-N's date is earlier than Year-1's date
-- (which is logically impossible for correct data).
UPDATE installment_fees sf
SET due_date = (
    SELECT (sf_y1.due_date + ((sf.year_number - 1) * INTERVAL '1 year'))::DATE
    FROM installment_fees sf_y1
    WHERE sf_y1.allocation_id = sf.allocation_id
      AND sf_y1.year_number   = 1
      AND sf_y1.sequence      = sf.sequence
)
WHERE sf.year_number > 1
  AND EXISTS (
      SELECT 1
      FROM installment_fees sf_y1
      WHERE sf_y1.allocation_id = sf.allocation_id
        AND sf_y1.year_number   = 1
        AND sf_y1.sequence      = sf.sequence
        AND sf.due_date < sf_y1.due_date   -- Year-N date is before Year-1 date: clearly wrong
  );
