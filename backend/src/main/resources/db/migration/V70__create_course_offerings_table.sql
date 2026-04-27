CREATE TABLE course_offerings (
    id                      BIGSERIAL PRIMARY KEY,
    term_instance_id        BIGINT NOT NULL REFERENCES term_instances(id),
    curriculum_version_id   BIGINT NOT NULL REFERENCES curriculum_versions(id),
    subject_id              BIGINT NOT NULL REFERENCES subjects(id),
    semester_number         INTEGER NOT NULL,
    faculty_id              BIGINT,
    section_label           VARCHAR(50),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (term_instance_id, curriculum_version_id, subject_id, semester_number)
);

CREATE INDEX idx_co_term_instance ON course_offerings(term_instance_id);
CREATE INDEX idx_co_semester_number ON course_offerings(semester_number);
CREATE INDEX idx_co_curriculum_version ON course_offerings(curriculum_version_id);
