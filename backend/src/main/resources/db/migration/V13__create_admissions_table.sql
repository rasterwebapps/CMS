CREATE TABLE admissions (
    id                      BIGSERIAL PRIMARY KEY,
    student_id              BIGINT                      NOT NULL REFERENCES students(id),
    academic_year_from      INTEGER                     NOT NULL,
    academic_year_to        INTEGER                     NOT NULL,
    application_date        DATE                        NOT NULL,
    status                  VARCHAR(255)                NOT NULL,
    declaration_place       VARCHAR(255),
    declaration_date        DATE,
    parent_consent_given    BOOLEAN,
    applicant_consent_given BOOLEAN,
    created_at              TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE    NOT NULL
);
