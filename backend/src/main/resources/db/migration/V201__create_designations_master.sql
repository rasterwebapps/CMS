-- V201: Replace Designation enum with a DB-managed designations master table.

-- 1. Create the designations table
CREATE TABLE designations (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_designations_name UNIQUE (name),
    CONSTRAINT uq_designations_code UNIQUE (code)
);

-- 2. Seed the 11 existing enum values (enum name becomes the code)
INSERT INTO designations (name, code) VALUES
  ('Professor',           'PROFESSOR'),
  ('Associate Professor', 'ASSOCIATE_PROFESSOR'),
  ('Assistant Professor', 'ASSISTANT_PROFESSOR'),
  ('Lecturer',            'LECTURER'),
  ('Senior Lecturer',     'SENIOR_LECTURER'),
  ('Lab Instructor',      'LAB_INSTRUCTOR'),
  ('Teaching Assistant',  'TEACHING_ASSISTANT'),
  ('Guest Faculty',       'GUEST_FACULTY'),
  ('Visiting Faculty',    'VISITING_FACULTY'),
  ('Nursing Tutor',       'NURSING_TUTOR'),
  ('Head of Speciality',  'HOD');

-- 3. Add designation_id FK to faculty (nullable first, populate, then enforce)
ALTER TABLE faculty ADD COLUMN designation_id BIGINT REFERENCES designations(id);

UPDATE faculty f
   SET designation_id = d.id
  FROM designations d
 WHERE f.designation = d.code;

ALTER TABLE faculty ALTER COLUMN designation_id SET NOT NULL;
ALTER TABLE faculty DROP COLUMN designation;

-- 4. Same for faculty_document_type_requirements (nullable — a rule may have no designation criterion)
ALTER TABLE faculty_document_type_requirements ADD COLUMN designation_id BIGINT REFERENCES designations(id);

UPDATE faculty_document_type_requirements fdr
   SET designation_id = d.id
  FROM designations d
 WHERE fdr.designation = d.code;

ALTER TABLE faculty_document_type_requirements DROP COLUMN designation;
