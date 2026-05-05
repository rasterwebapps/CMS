CREATE TABLE ppe_items (
    id                    BIGSERIAL PRIMARY KEY,
    lab_id                BIGINT        NOT NULL REFERENCES labs(id),
    name                  VARCHAR(255)  NOT NULL,
    category              VARCHAR(50)   NOT NULL,
    total_quantity        INTEGER       NOT NULL,
    available_quantity    INTEGER       NOT NULL,
    minimum_required      INTEGER       NOT NULL,
    condition             VARCHAR(30)   NOT NULL,
    last_inspection_date  DATE,
    next_inspection_date  DATE,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

