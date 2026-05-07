-- Replace lab_schedules.semester_id with term_instance_id.
-- ODD terms start June-Nov (month >= 6), EVEN terms start Dec-May (month < 6).

ALTER TABLE lab_schedules ADD COLUMN term_instance_id BIGINT;

UPDATE lab_schedules ls
SET term_instance_id = (
    SELECT ti.id
    FROM term_instances ti
    JOIN semesters s ON s.academic_year_id = ti.academic_year_id
    WHERE s.id = ls.semester_id
      AND (
          (EXTRACT(MONTH FROM s.start_date) >= 6 AND ti.term_type = 'ODD')
          OR
          (EXTRACT(MONTH FROM s.start_date) < 6 AND ti.term_type = 'EVEN')
      )
    LIMIT 1
);

ALTER TABLE lab_schedules ALTER COLUMN term_instance_id SET NOT NULL;

ALTER TABLE lab_schedules
    ADD CONSTRAINT fk_lab_schedules_term_instance
    FOREIGN KEY (term_instance_id) REFERENCES term_instances(id);

ALTER TABLE lab_schedules DROP COLUMN semester_id;
