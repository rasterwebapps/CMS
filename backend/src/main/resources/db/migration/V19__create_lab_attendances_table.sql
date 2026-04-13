CREATE TABLE lab_attendances (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT                      NOT NULL REFERENCES students(id),
    course_id        BIGINT                      NOT NULL REFERENCES courses(id),
    experiment_id    BIGINT                      REFERENCES experiments(id),
    lab_id           BIGINT                      NOT NULL REFERENCES labs(id),
    lab_schedule_id  BIGINT                      REFERENCES lab_schedules(id),
    lab_batch        VARCHAR(255)                NOT NULL,
    date             DATE                        NOT NULL,
    status           VARCHAR(255)                NOT NULL,
    remarks          VARCHAR(255),
    marked_by        BIGINT                      REFERENCES faculty(id),
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
