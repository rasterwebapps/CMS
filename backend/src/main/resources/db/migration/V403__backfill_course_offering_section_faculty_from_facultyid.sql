-- Backfills course_offering_section_faculty from course_offerings.faculty_id (about to be
-- dropped in V404) before it's gone. Cohort resolution mirrors
-- CourseOfferingSectionFacultyService.resolveCohorts exactly: curriculum version's
-- program+course match, filtered to cohorts with a student enrolled at that offering's
-- term_number this term instance. Mirrors that method's existing assumption that
-- curriculum_versions.course_id is set (cv.course_id IS NOT NULL) -- a program-wide curriculum
-- version (course_id NULL) already can't resolve cohorts via the existing Java path either, so
-- offerings under one are left alone here too rather than guessing new behavior for them.

-- 1. Section-scoped rows: one per active section lacking its own override, for cohorts whose
--    Theory delivery is split.
INSERT INTO course_offering_section_faculty (course_offering_id, cohort_id, cohort_section_id, faculty_id, created_at, updated_at)
SELECT DISTINCT co.id, cra.cohort_id, cs.id, co.faculty_id, NOW(), NOW()
FROM course_offerings co
JOIN curriculum_versions cv ON cv.id = co.curriculum_version_id
JOIN cohorts c ON c.course_id = cv.course_id
JOIN courses crs ON crs.id = c.course_id AND crs.program_id = cv.program_id
JOIN cohort_room_allocations cra ON cra.cohort_id = c.id AND cra.term_instance_id = co.term_instance_id AND cra.status = 'COMMITTED'
JOIN cohort_sections cs ON cs.cohort_room_allocation_id = cra.id AND cs.is_active = TRUE
WHERE co.faculty_id IS NOT NULL
  AND cv.course_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM student_term_enrollments ste
    WHERE ste.term_instance_id = co.term_instance_id AND ste.cohort_id = c.id AND ste.status = 'ENROLLED'
  )
  AND EXISTS (
    SELECT 1 FROM student_term_enrollments ste2
    WHERE ste2.term_instance_id = co.term_instance_id AND ste2.cohort_id = c.id AND ste2.term_number = co.term_number
  )
  AND NOT EXISTS (
    SELECT 1 FROM course_offering_section_faculty existing
    WHERE existing.course_offering_id = co.id AND existing.cohort_section_id = cs.id
  );

-- 2. Whole-cohort rows: for cohorts with no active section split.
INSERT INTO course_offering_section_faculty (course_offering_id, cohort_id, cohort_section_id, faculty_id, created_at, updated_at)
SELECT DISTINCT co.id, c.id, NULL::bigint, co.faculty_id, NOW(), NOW()
FROM course_offerings co
JOIN curriculum_versions cv ON cv.id = co.curriculum_version_id
JOIN cohorts c ON c.course_id = cv.course_id
JOIN courses crs ON crs.id = c.course_id AND crs.program_id = cv.program_id
WHERE co.faculty_id IS NOT NULL
  AND cv.course_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM student_term_enrollments ste
    WHERE ste.term_instance_id = co.term_instance_id AND ste.cohort_id = c.id AND ste.status = 'ENROLLED'
  )
  AND EXISTS (
    SELECT 1 FROM student_term_enrollments ste2
    WHERE ste2.term_instance_id = co.term_instance_id AND ste2.cohort_id = c.id AND ste2.term_number = co.term_number
  )
  AND NOT EXISTS (
    SELECT 1 FROM cohort_room_allocations cra
    JOIN cohort_sections cs ON cs.cohort_room_allocation_id = cra.id AND cs.is_active = TRUE
    WHERE cra.cohort_id = c.id AND cra.term_instance_id = co.term_instance_id AND cra.status = 'COMMITTED'
  )
  AND NOT EXISTS (
    SELECT 1 FROM course_offering_section_faculty existing
    WHERE existing.course_offering_id = co.id AND existing.cohort_id = c.id AND existing.cohort_section_id IS NULL
  );
