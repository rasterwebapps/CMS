-- Real, enforceable lab/clinical batch splitting (e.g. 60 students -> 3 batches of 20), replacing
-- the free-text, unvalidated batch_name/lab_batch labels on lab_schedules/lab_attendances. A
-- batch is scoped to a specific term's CourseOffering (not the curriculum mapping) since it's a
-- per-term roster split, not curriculum-design metadata — the same subject's Sem I batches must
-- not bleed into a different term/cohort's batches. Capacity is enforced at the service layer,
-- matching this codebase's existing convention (no DB-level seat/capacity constraints elsewhere,
-- e.g. Cohort seat limits are enforced in AcademicYearService).

CREATE TABLE batches (
    id                      BIGSERIAL PRIMARY KEY,
    course_offering_id      BIGINT NOT NULL REFERENCES course_offerings(id),
    name                    VARCHAR(100) NOT NULL,
    capacity                INTEGER NOT NULL,
    term_instance_id        BIGINT NOT NULL REFERENCES term_instances(id),
    coordinator_faculty_id  BIGINT REFERENCES faculty(id),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (course_offering_id, name)
);

CREATE TABLE batch_students (
    batch_id    BIGINT NOT NULL REFERENCES batches(id),
    student_id  BIGINT NOT NULL REFERENCES students(id),
    PRIMARY KEY (batch_id, student_id)
);

CREATE INDEX idx_batch_students_student_id ON batch_students(student_id);
CREATE INDEX idx_batches_course_offering_id ON batches(course_offering_id);
