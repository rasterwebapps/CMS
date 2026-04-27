CREATE TABLE student_marks (
    id BIGSERIAL PRIMARY KEY,
    exam_event_id BIGINT NOT NULL REFERENCES exam_events(id),
    course_registration_id BIGINT NOT NULL REFERENCES course_registrations(id),
    mark_status VARCHAR(20) NOT NULL DEFAULT 'ABSENT',
    marks_obtained NUMERIC(10, 2),
    remarks VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_student_marks_event_registration UNIQUE (exam_event_id, course_registration_id)
);
