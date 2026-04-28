CREATE TABLE term_instances (
    id                  BIGSERIAL PRIMARY KEY,
    academic_year_id    BIGINT NOT NULL REFERENCES academic_years(id),
    term_type           VARCHAR(10) NOT NULL CHECK (term_type IN ('ODD', 'EVEN')),
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (academic_year_id, term_type)
);
