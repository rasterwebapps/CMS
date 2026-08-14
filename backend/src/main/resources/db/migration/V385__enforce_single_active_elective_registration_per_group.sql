-- DB-level guard against a student ending up REGISTERED to more than one course offering within
-- the same choice-based elective group for a term. The two write paths in
-- CourseRegistrationServiceImpl (assignElectiveChoice, bulkAssignElectiveChoice) already enforce
-- this in application code, but nothing at the schema level stops a different write path (a
-- script, a future endpoint, a manual DB fix-up) from creating a second active registration in
-- the same group. Skeleton Builder places every sibling offering in a group at the same
-- day/period, so a double registration would show the student needing to be in two rooms at once
-- and would double-count them in TimetableStaffingService's per-offering capacity check.
--
-- elective_group_id isn't a column on course_registrations — it's only reachable via
-- course_offerings.curriculum_term_course_id -> curriculum_term_courses.elective_group_id.
-- Postgres unique indexes can't enforce across a join, so this migration denormalizes the value
-- onto course_registrations, kept in sync by a trigger (not mapped in the JPA entity — DB-only),
-- so a real partial unique index can enforce "at most one active registration per student per
-- elective group" atomically regardless of which code path writes the row.

ALTER TABLE course_registrations ADD COLUMN elective_group_id BIGINT REFERENCES curriculum_elective_groups(id);

CREATE INDEX idx_course_registrations_elective_group_id ON course_registrations(elective_group_id);

UPDATE course_registrations cr
SET elective_group_id = ctc.elective_group_id
FROM course_offerings co
JOIN curriculum_term_courses ctc ON ctc.id = co.curriculum_term_course_id
WHERE cr.course_offering_id = co.id
  AND ctc.elective_group_id IS NOT NULL;

CREATE OR REPLACE FUNCTION sync_course_registration_elective_group() RETURNS TRIGGER AS $$
BEGIN
    SELECT ctc.elective_group_id INTO NEW.elective_group_id
    FROM course_offerings co
    JOIN curriculum_term_courses ctc ON ctc.id = co.curriculum_term_course_id
    WHERE co.id = NEW.course_offering_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_course_registration_sync_elective_group
    BEFORE INSERT OR UPDATE OF course_offering_id ON course_registrations
    FOR EACH ROW
    EXECUTE FUNCTION sync_course_registration_elective_group();

-- At most one active (non-DROPPED) registration per student per elective group. Partial index,
-- not a table constraint: DROPPED registrations must be allowed to coexist (history), and
-- non-elective registrations (elective_group_id IS NULL) never participate.
CREATE UNIQUE INDEX uq_course_registrations_active_elective_group
    ON course_registrations (student_term_enrollment_id, elective_group_id)
    WHERE status <> 'DROPPED' AND elective_group_id IS NOT NULL;
