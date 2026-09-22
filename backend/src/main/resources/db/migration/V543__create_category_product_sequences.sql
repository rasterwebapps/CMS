-- Per-category running counter backing auto-generated Product codes (<CategoryShortCode>-<seq>,
-- e.g. STA-000001) — one row per category, incremented under a pessimistic lock by
-- ProductCodeGeneratorService, mirroring roll_number_sequences (V-era RollNumberGeneratorService).

CREATE TABLE category_product_sequences (
    id              BIGSERIAL   PRIMARY KEY,
    category_id     BIGINT      NOT NULL REFERENCES categories(id),
    last_sequence   BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_category_product_sequences_category UNIQUE (category_id)
);
