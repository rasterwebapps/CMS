CREATE TABLE safety_audits (
    id               BIGSERIAL PRIMARY KEY,
    lab_id           BIGINT        NOT NULL REFERENCES labs(id),
    auditor_name     VARCHAR(255)  NOT NULL,
    audit_date       DATE          NOT NULL,
    next_audit_date  DATE,
    overall_rating   VARCHAR(30)   NOT NULL,
    findings         TEXT,
    recommendations  TEXT,
    status           VARCHAR(30)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

