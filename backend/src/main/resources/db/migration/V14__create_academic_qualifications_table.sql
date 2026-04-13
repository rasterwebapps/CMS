CREATE TABLE academic_qualifications (
    id                      BIGSERIAL PRIMARY KEY,
    admission_id            BIGINT                      NOT NULL REFERENCES admissions(id),
    qualification_type      VARCHAR(255)                NOT NULL,
    school_name             VARCHAR(255),
    major_subject           VARCHAR(255),
    total_marks             INTEGER,
    percentage              NUMERIC(5,2),
    month_and_year_of_passing VARCHAR(255),
    university_or_board     VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE    NOT NULL
);
