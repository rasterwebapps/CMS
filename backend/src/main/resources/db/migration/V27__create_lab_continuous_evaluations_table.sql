CREATE TABLE lab_continuous_evaluations (
    id                  BIGSERIAL PRIMARY KEY,
    experiment_id       BIGINT                      NOT NULL REFERENCES experiments(id),
    student_id          BIGINT                      NOT NULL REFERENCES students(id),
    record_marks        INTEGER,
    viva_marks          INTEGER,
    performance_marks   INTEGER,
    total_marks         INTEGER,
    evaluation_date     DATE,
    evaluated_by        VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE    NOT NULL
);
