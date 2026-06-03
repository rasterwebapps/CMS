-- Rename the departments table to specialities
ALTER TABLE departments RENAME TO specialities;

-- Rename FK columns in all referencing tables
ALTER TABLE faculty RENAME COLUMN department_id TO speciality_id;
ALTER TABLE labs RENAME COLUMN department_id TO speciality_id;
ALTER TABLE subjects RENAME COLUMN department_id TO speciality_id;
ALTER TABLE safety_guidelines RENAME COLUMN department_id TO speciality_id;
ALTER TABLE faculty_document_type_requirements RENAME COLUMN department_id TO speciality_id;
ALTER TABLE students RENAME COLUMN specialization_department_id TO speciality_id;

-- Rename index
ALTER INDEX IF EXISTS idx_fdtr_department_id RENAME TO idx_fdtr_speciality_id;
