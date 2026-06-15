-- V215: Auto-assign existing students to their cohorts.
--
-- All 157 students currently have cohort_id = NULL because cohort assignment
-- was never wired into the admission flow. Cohorts already exist for every
-- admission academic year; students just need to be linked.
--
-- Match rule:
--   student.program_id → courses.program_id  (finds the course for the program)
--   admissions.joining_academic_year_id = cohorts.admission_academic_year_id
--   cohorts.course_id = courses.id
--
-- Only affects students where cohort_id IS NULL — safe to re-run.

UPDATE students
SET    cohort_id = match.cohort_id
FROM (
    SELECT s.id AS student_id, co.id AS cohort_id
    FROM   students   s
    JOIN   admissions a  ON a.student_id  = s.id
    JOIN   courses    cu ON cu.program_id = s.program_id
    JOIN   cohorts    co ON co.course_id  = cu.id
                        AND co.admission_academic_year_id = a.joining_academic_year_id
    WHERE  s.cohort_id IS NULL
) match
WHERE students.id = match.student_id;
