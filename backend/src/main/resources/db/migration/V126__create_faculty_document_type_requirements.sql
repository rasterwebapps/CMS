-- V126: Faculty Document Type Requirements Configuration
-- Allows admins to configure which documents are required from faculty
-- based on any of: designation, speciality, or highest qualification.
-- A document is required for a faculty member when ANY matching rule is found.

CREATE TABLE faculty_document_type_requirements (
    id              BIGSERIAL    PRIMARY KEY,
    document_type   VARCHAR(80)  NOT NULL,
    designation     VARCHAR(50),
    speciality_id   BIGINT       REFERENCES specialities(id) ON DELETE CASCADE,
    qualification   VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fdtr_at_least_one_criterion
        CHECK (designation IS NOT NULL OR speciality_id IS NOT NULL OR qualification IS NOT NULL)
);

CREATE INDEX idx_fdtr_designation   ON faculty_document_type_requirements(designation)   WHERE designation   IS NOT NULL;
CREATE INDEX idx_fdtr_speciality_id ON faculty_document_type_requirements(speciality_id) WHERE speciality_id IS NOT NULL;
CREATE INDEX idx_fdtr_qualification ON faculty_document_type_requirements(qualification) WHERE qualification IS NOT NULL;

ALTER TABLE faculty ADD COLUMN IF NOT EXISTS highest_qualification VARCHAR(50);
