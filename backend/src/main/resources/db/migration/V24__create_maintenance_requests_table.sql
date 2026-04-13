CREATE TABLE maintenance_requests (
    id               BIGSERIAL PRIMARY KEY,
    equipment_id     BIGINT                      NOT NULL REFERENCES equipment(id),
    title            VARCHAR(255)                NOT NULL,
    description      TEXT,
    maintenance_type VARCHAR(255)                NOT NULL,
    priority         VARCHAR(255)                NOT NULL,
    status           VARCHAR(255)                NOT NULL,
    requested_by     BIGINT                      REFERENCES faculty(id),
    request_date     DATE                        NOT NULL,
    scheduled_date   DATE,
    completion_date  DATE,
    assigned_to      BIGINT                      REFERENCES faculty(id),
    estimated_cost   NUMERIC(10,2),
    actual_cost      NUMERIC(10,2),
    resolution_notes TEXT,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
