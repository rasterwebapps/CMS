CREATE TABLE semesters (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)                NOT NULL,
    academic_year_id BIGINT                      NOT NULL REFERENCES academic_years(id),
    start_date       DATE                        NOT NULL,
    end_date         DATE                        NOT NULL,
    semester_number  INTEGER                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
