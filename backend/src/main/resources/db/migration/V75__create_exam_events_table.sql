CREATE TABLE exam_events (
    id BIGSERIAL PRIMARY KEY,
    exam_session_id BIGINT NOT NULL REFERENCES exam_sessions(id),
    course_offering_id BIGINT NOT NULL REFERENCES course_offerings(id),
    exam_date DATE,
    max_marks NUMERIC(10, 2) NOT NULL,
    pass_marks NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_exam_events_session_offering UNIQUE (exam_session_id, course_offering_id)
);
