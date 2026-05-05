CREATE TABLE safety_training_records (
    id              BIGSERIAL PRIMARY KEY,
    trainee         VARCHAR(255)  NOT NULL,
    trainee_type    VARCHAR(20)   NOT NULL,
    training_title  VARCHAR(255)  NOT NULL,
    description     TEXT,
    lab_id          BIGINT REFERENCES labs(id),
    conducted_by    VARCHAR(255)  NOT NULL,
    training_date   DATE          NOT NULL,
    valid_until     DATE,
    status          VARCHAR(20)   NOT NULL,
    score           INTEGER,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

