-- Removed at the user's request: this was a free-text, disconnected label on the Course Offering
-- itself, redundant with (and never validated against) the real cohort_sections.section_label
-- that now drives the Section Faculty feature (V391/V392). Nothing else read this column --
-- verified against Java code before writing this DROP, per the migration column-verification gate.
ALTER TABLE course_offerings DROP COLUMN section_label;
