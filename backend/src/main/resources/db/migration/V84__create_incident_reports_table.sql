CREATE TABLE incident_reports (
    id                   BIGSERIAL PRIMARY KEY,
    lab_id               BIGINT        NOT NULL REFERENCES labs(id),
    reported_by          VARCHAR(255)  NOT NULL,
    reported_by_email    VARCHAR(255),
    incident_date        DATE          NOT NULL,
    incident_time        TIME,
    title                VARCHAR(255)  NOT NULL,
    description          TEXT,
    severity             VARCHAR(20)   NOT NULL,
    incident_type        VARCHAR(30)   NOT NULL,
    status               VARCHAR(30)   NOT NULL,
    action_taken         TEXT,
    investigated_by      VARCHAR(255),
    resolved_date        DATE,
    preventive_measures  TEXT,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

