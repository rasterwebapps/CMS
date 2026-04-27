CREATE TABLE cohorts (
    id                              BIGSERIAL PRIMARY KEY,
    program_id                      BIGINT NOT NULL REFERENCES programs(id),
    admission_academic_year_id      BIGINT NOT NULL REFERENCES academic_years(id),
    expected_graduation_academic_year_id BIGINT REFERENCES academic_years(id),
    cohort_code                     VARCHAR(50) NOT NULL UNIQUE,
    display_name                    VARCHAR(200) NOT NULL,
    status                          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (program_id, admission_academic_year_id)
);
CREATE INDEX idx_cohorts_program ON cohorts(program_id);
CREATE INDEX idx_cohorts_admission_ay ON cohorts(admission_academic_year_id);
