-- Inventory Management (Release 3) — first Phase 1 slice: item Categories (hierarchical) and
-- Units of Measure. Full design: docs/inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md §2.
-- Product/CategoryAttribute/etc. deliberately not created yet — they land in the next slice.

CREATE TABLE categories (
    id                  BIGSERIAL     PRIMARY KEY,
    name                VARCHAR(150)  NOT NULL,
    parent_category_id  BIGINT        REFERENCES categories(id),
    description         VARCHAR(500),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    -- Sibling name uniqueness for categories that DO have a parent — Postgres UNIQUE treats each
    -- NULL as distinct, so this alone does not catch duplicate top-level (parent IS NULL) names;
    -- the partial index below covers that case.
    CONSTRAINT uq_categories_name_parent UNIQUE (parent_category_id, name)
);

CREATE UNIQUE INDEX uq_categories_name_root ON categories(name) WHERE parent_category_id IS NULL;
CREATE INDEX idx_categories_parent ON categories(parent_category_id);

CREATE TABLE uoms (
    id          BIGSERIAL     PRIMARY KEY,
    code        VARCHAR(20)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_uoms_code UNIQUE (code),
    CONSTRAINT uq_uoms_name UNIQUE (name)
);
