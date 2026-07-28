-- Round 2 of the Timetable planner/calendar initiative (see OC-87/OC-88). First piece: unit-wise
-- syllabus allocation. Units are curriculum-level (hang off curriculum_term_courses, confirmed
-- exact table/column name from V283 syllabi.curriculum_term_course_id), not per course-offering --
-- one shared unit list is reused by every section/faculty teaching that subject in a given term,
-- matching how theory_hours/lab_hours/clinical_hours already work on the same parent row.

CREATE TABLE syllabus_units (
    id                        BIGSERIAL PRIMARY KEY,
    curriculum_term_course_id BIGINT NOT NULL REFERENCES curriculum_term_courses(id),
    unit_number               INTEGER NOT NULL,
    title                     VARCHAR(200) NOT NULL,
    planned_hours             INTEGER,
    description               VARCHAR(1000),
    sort_order                INTEGER,
    is_active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_syllabus_units_course_unit_number UNIQUE (curriculum_term_course_id, unit_number)
);

CREATE INDEX idx_syllabus_units_curriculum_term_course ON syllabus_units(curriculum_term_course_id);
