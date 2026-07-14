-- Adds an active/inactive status flag to subjects, matching the standard master
-- pattern already used by courses and specialities (both have is_active). Subjects
-- previously had no way to be soft-retired; the new Subject Master screen's
-- Activate/Deactivate action needs this column.

ALTER TABLE subjects ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
