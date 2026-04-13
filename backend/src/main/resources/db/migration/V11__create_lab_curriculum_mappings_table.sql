CREATE TABLE lab_curriculum_mappings (
    id                  BIGSERIAL PRIMARY KEY,
    experiment_id       BIGINT                      NOT NULL REFERENCES experiments(id),
    outcome_type        VARCHAR(255)                NOT NULL,
    outcome_code        VARCHAR(255)                NOT NULL,
    outcome_description VARCHAR(1000),
    mapping_level       VARCHAR(255)                NOT NULL,
    justification       VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
    UNIQUE (experiment_id, outcome_type, outcome_code)
);
