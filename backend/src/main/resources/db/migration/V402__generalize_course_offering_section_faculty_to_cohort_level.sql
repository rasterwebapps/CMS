-- Generalizes course_offering_section_faculty to cover the "no section split" case too, not just
-- per-section overrides -- see CourseOfferingSectionFaculty's javadoc. Every row now carries a
-- cohort_id (a single CourseOffering can be shared by more than one cohort, each assigned
-- independently); cohort_section_id becomes nullable, where NULL means "whole cohort".

ALTER TABLE course_offering_section_faculty ADD COLUMN cohort_id BIGINT REFERENCES cohorts(id);

-- Backfill cohort_id for existing (section-scoped) rows via cohort_section -> cohort_room_allocation -> cohort.
UPDATE course_offering_section_faculty cosf
SET cohort_id = cra.cohort_id
FROM cohort_sections cs
JOIN cohort_room_allocations cra ON cra.id = cs.cohort_room_allocation_id
WHERE cosf.cohort_section_id = cs.id;

ALTER TABLE course_offering_section_faculty ALTER COLUMN cohort_id SET NOT NULL;
ALTER TABLE course_offering_section_faculty ALTER COLUMN cohort_section_id DROP NOT NULL;

CREATE INDEX idx_course_offering_section_faculty_cohort_id ON course_offering_section_faculty(cohort_id);

-- Exactly one whole-cohort row (cohort_section_id NULL) per (offering, cohort) -- a plain
-- multi-column UNIQUE constraint can't enforce this since Postgres treats repeated NULLs as
-- distinct. The existing (course_offering_id, cohort_section_id) UNIQUE constraint already
-- covers the section-scoped case (a cohort_section_id, when set, already implies exactly one cohort).
CREATE UNIQUE INDEX ux_course_offering_section_faculty_whole_cohort
    ON course_offering_section_faculty(course_offering_id, cohort_id) WHERE cohort_section_id IS NULL;
