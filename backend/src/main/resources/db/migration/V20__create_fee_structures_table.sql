CREATE TABLE fee_structures (
    id               BIGSERIAL PRIMARY KEY,
    program_id       BIGINT                      NOT NULL REFERENCES programs(id),
    academic_year_id BIGINT                      NOT NULL REFERENCES academic_years(id),
    fee_type         VARCHAR(255)                NOT NULL,
    amount           NUMERIC(10,2)               NOT NULL,
    description      VARCHAR(255),
    is_mandatory     BOOLEAN,
    is_active        BOOLEAN,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
