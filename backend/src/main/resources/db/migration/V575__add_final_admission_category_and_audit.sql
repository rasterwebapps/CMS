-- V575: Widen students.admission_category, add students.final_admission_category,
-- and create student_admission_category_history audit table.

ALTER TABLE students
    ALTER COLUMN admission_category TYPE VARCHAR(40);

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS final_admission_category VARCHAR(40);

UPDATE students
SET final_admission_category = admission_category
WHERE final_admission_category IS NULL
  AND admission_category IS NOT NULL;

CREATE TABLE IF NOT EXISTS student_admission_category_history (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    old_category    VARCHAR(40) NOT NULL,
    new_category    VARCHAR(40) NOT NULL,
    reason          TEXT NOT NULL,
    changed_by      VARCHAR(255),
    changed_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_student_adm_cat_hist_student_id
    ON student_admission_category_history(student_id);
