CREATE TABLE lab_schedules (
    id           BIGSERIAL PRIMARY KEY,
    lab_id       BIGINT                      NOT NULL REFERENCES labs(id),
    course_id    BIGINT                      NOT NULL REFERENCES courses(id),
    faculty_id   BIGINT                      NOT NULL REFERENCES faculty(id),
    lab_slot_id  BIGINT                      NOT NULL REFERENCES lab_slots(id),
    batch_name   VARCHAR(255)                NOT NULL,
    day_of_week  VARCHAR(255)                NOT NULL,
    semester_id  BIGINT                      NOT NULL REFERENCES semesters(id),
    is_active    BOOLEAN,
    created_at   TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE    NOT NULL
);
