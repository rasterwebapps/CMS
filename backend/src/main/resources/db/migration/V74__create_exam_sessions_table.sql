CREATE TABLE exam_sessions (
    id BIGSERIAL PRIMARY KEY,
    term_instance_id BIGINT NOT NULL REFERENCES term_instances(id),
    session_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_exam_sessions_term_type UNIQUE (term_instance_id, session_type)
);
