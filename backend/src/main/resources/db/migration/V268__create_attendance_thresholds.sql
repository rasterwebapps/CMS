-- Per-offering, per-component minimum attendance percentage (e.g. 80% Theory / 100% Clinical),
-- replacing the single hardcoded 75% global constant. Keyed on the curriculum-term mapping
-- (not the Subject master) so thresholds can vary by semester the same way hours do. Absence of
-- a row for a given (curriculum_term_course_id, attendance_type) means "use the 75% app default",
-- so no seed data is required here.

CREATE TABLE attendance_thresholds (
    id                          BIGSERIAL PRIMARY KEY,
    curriculum_term_course_id   BIGINT NOT NULL REFERENCES curriculum_term_courses(id),
    attendance_type             VARCHAR(20) NOT NULL,
    min_percentage              NUMERIC(5,2) NOT NULL,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (curriculum_term_course_id, attendance_type)
);
