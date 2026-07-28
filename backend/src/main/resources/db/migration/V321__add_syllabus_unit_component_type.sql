-- A unit's planned hours must count against ONE specific hour bucket on its parent
-- curriculum_term_courses row (theory_hours/lab_hours/clinical_hours are tracked separately there,
-- confirmed exact column names from V266) so SyllabusUnitService can validate that a subject's
-- units never sum above its declared totals per bucket. Defaults existing rows to THEORY (the only
-- bucket populated for every curriculum_term_courses row that has units so far in this dataset).

ALTER TABLE syllabus_units ADD COLUMN component_type VARCHAR(20) NOT NULL DEFAULT 'THEORY';
