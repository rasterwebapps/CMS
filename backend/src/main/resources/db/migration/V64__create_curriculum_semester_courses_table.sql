CREATE TABLE curriculum_semester_courses (
    id                      BIGSERIAL PRIMARY KEY,
    curriculum_version_id   BIGINT NOT NULL REFERENCES curriculum_versions(id),
    semester_number         INTEGER NOT NULL,
    subject_id              BIGINT NOT NULL REFERENCES subjects(id),
    sort_order              INTEGER,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (curriculum_version_id, semester_number, subject_id)
);
