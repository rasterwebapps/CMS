CREATE TABLE intake_rules (
    id                              BIGSERIAL PRIMARY KEY,
    program_id                      BIGINT NOT NULL REFERENCES programs(id),
    admission_window_start_date     DATE NOT NULL,
    admission_window_end_date       DATE NOT NULL,
    mapped_academic_year_id         BIGINT NOT NULL REFERENCES academic_years(id),
    mapped_start_term_type          VARCHAR(10) NOT NULL,
    starting_semester_number        INTEGER NOT NULL DEFAULT 1,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_intake_rules_program ON intake_rules(program_id);
