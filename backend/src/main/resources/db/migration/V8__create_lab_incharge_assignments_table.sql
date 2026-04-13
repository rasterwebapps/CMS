CREATE TABLE lab_incharge_assignments (
    id            BIGSERIAL PRIMARY KEY,
    lab_id        BIGINT                      NOT NULL REFERENCES labs(id),
    assignee_id   BIGINT                      NOT NULL,
    assignee_name VARCHAR(255)                NOT NULL,
    role          VARCHAR(255)                NOT NULL,
    assigned_date DATE                        NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE    NOT NULL
);
