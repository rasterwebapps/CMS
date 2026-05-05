CREATE TABLE safety_guidelines (
    id                BIGSERIAL PRIMARY KEY,
    lab_id            BIGINT REFERENCES labs(id),
    department_id     BIGINT REFERENCES departments(id),
    title             VARCHAR(255)  NOT NULL,
    description       TEXT,
    category          VARCHAR(50)   NOT NULL,
    priority          VARCHAR(20)   NOT NULL,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    effective_date    DATE          NOT NULL,
    review_date       DATE,
    created_by        VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

