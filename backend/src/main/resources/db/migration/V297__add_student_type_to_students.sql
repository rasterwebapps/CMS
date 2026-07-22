-- R2-M4.0: studentType becomes a persistent, mutable attribute of an admitted Student, not just
-- a value on Enquiry that's discarded after conversion. Nullable — pre-existing students may have
-- no matching enquiry (e.g. seeded/manually created) or an enquiry with student_type never set.
-- Backfill only touches rows we can trace unambiguously via enquiries.converted_student_id, the
-- same link FeeFinalizationService already relies on for StudentFeeAllocation.hasHostelFee.

ALTER TABLE students ADD COLUMN student_type VARCHAR(20);

UPDATE students s
SET student_type = e.student_type
FROM enquiries e
WHERE e.converted_student_id = s.id
  AND e.student_type IS NOT NULL
  AND s.student_type IS NULL;
