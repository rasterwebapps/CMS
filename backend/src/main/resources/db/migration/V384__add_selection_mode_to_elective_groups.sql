-- V384: Institution-decided elective assignment (vs. per-student choice)
-- Most colleges elect one option among an elective group's offerings centrally rather than
-- letting each student choose — this column lets an admin flip that per elective group.
-- Existing groups default to STUDENT_CHOICE, matching current behavior exactly.
ALTER TABLE curriculum_elective_groups
    ADD COLUMN selection_mode VARCHAR(30) NOT NULL DEFAULT 'STUDENT_CHOICE';
