-- Switch cohorts from program-based to course-based

ALTER TABLE cohorts ADD COLUMN course_id BIGINT REFERENCES courses(id);

-- Map existing cohorts: take the first (lowest) course id for each program
UPDATE cohorts c
SET course_id = (
    SELECT MIN(co.id) FROM courses co WHERE co.program_id = c.program_id
);

-- Remove enrollment data that references cohorts that couldn't be mapped
DELETE FROM student_term_enrollments
WHERE cohort_id IN (SELECT id FROM cohorts WHERE course_id IS NULL);

UPDATE students SET cohort_id = NULL
WHERE cohort_id IN (SELECT id FROM cohorts WHERE course_id IS NULL);

DELETE FROM cohorts WHERE course_id IS NULL;

ALTER TABLE cohorts ALTER COLUMN course_id SET NOT NULL;

ALTER TABLE cohorts DROP CONSTRAINT IF EXISTS cohorts_program_id_admission_academic_year_id_key;
ALTER TABLE cohorts ADD CONSTRAINT cohorts_course_id_ay_id_key
    UNIQUE (course_id, admission_academic_year_id);

ALTER TABLE cohorts DROP COLUMN program_id;
