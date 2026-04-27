CREATE TABLE curriculum_versions (
    id                              BIGSERIAL PRIMARY KEY,
    program_id                      BIGINT NOT NULL REFERENCES programs(id),
    version_name                    VARCHAR(100) NOT NULL,
    effective_from_academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL
);
