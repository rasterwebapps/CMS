ALTER TABLE enquiries ADD COLUMN academic_year_id BIGINT REFERENCES academic_years(id);

CREATE INDEX idx_enquiries_academic_year_id ON enquiries(academic_year_id);

-- Backfill from the linked admission's joining academic year where the enquiry has already converted.
UPDATE enquiries e
SET academic_year_id = a.joining_academic_year_id
FROM admissions a
WHERE a.enquiry_id = e.id
  AND e.academic_year_id IS NULL;

-- Backfill remaining (not yet converted) enquiries by matching enquiry_date against
-- each program's active intake window.
UPDATE enquiries e
SET academic_year_id = ir.mapped_academic_year_id
FROM intake_rules ir
WHERE ir.program_id = e.program_id
  AND ir.is_active = true
  AND e.enquiry_date BETWEEN ir.admission_window_start_date AND ir.admission_window_end_date
  AND e.academic_year_id IS NULL
  AND e.program_id IS NOT NULL;
