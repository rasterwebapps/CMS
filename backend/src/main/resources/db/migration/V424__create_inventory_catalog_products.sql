-- Inventory Management (Release 3) — second Phase 1 slice: CategoryAttribute (per-category EAV
-- attribute definitions) and Product (with its aliases and attribute values). ProductImage is
-- deliberately not created here — deferred along with the MinIO upload plumbing it needs, see
-- docs/inventory-management/DECISION_LOG.md's 2026-09-07 "Product slice" entry.
-- Full design: docs/inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md §2.

CREATE TABLE category_attributes (
    id             BIGSERIAL     PRIMARY KEY,
    category_id    BIGINT        NOT NULL REFERENCES categories(id),
    name           VARCHAR(100)  NOT NULL,
    data_type      VARCHAR(20)   NOT NULL,
    -- Comma-separated allowed values, only meaningful when data_type = 'ENUM'.
    enum_options   VARCHAR(1000),
    is_required    BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order  INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_category_attributes_name UNIQUE (category_id, name),
    CONSTRAINT chk_category_attributes_data_type CHECK (data_type IN ('TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'ENUM'))
);

CREATE INDEX idx_category_attributes_category ON category_attributes(category_id);

CREATE TABLE products (
    id                     BIGSERIAL      PRIMARY KEY,
    product_code           VARCHAR(50)    NOT NULL,
    product_name           VARCHAR(200)   NOT NULL,
    category_id            BIGINT         NOT NULL REFERENCES categories(id),
    base_uom_id            BIGINT         NOT NULL REFERENCES uoms(id),
    reorder_level          NUMERIC(14,3),
    reorder_qty            NUMERIC(14,3),
    is_asset               BOOLEAN        NOT NULL DEFAULT FALSE,
    is_consumable          BOOLEAN        NOT NULL DEFAULT TRUE,
    is_service             BOOLEAN        NOT NULL DEFAULT FALSE,
    is_loanable            BOOLEAN        NOT NULL DEFAULT FALSE,
    depreciation_rate      NUMERIC(5,2),
    warranty_period_months INTEGER,
    description            VARCHAR(1000),
    is_active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_code UNIQUE (product_code),
    CONSTRAINT uq_products_name_category UNIQUE (category_id, product_name)
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_base_uom ON products(base_uom_id);

CREATE TABLE product_aliases (
    id           BIGSERIAL     PRIMARY KEY,
    product_id   BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    alias_name   VARCHAR(200)  NOT NULL
);

CREATE INDEX idx_product_aliases_product ON product_aliases(product_id);

CREATE TABLE product_attribute_values (
    id            BIGSERIAL     PRIMARY KEY,
    product_id    BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_id  BIGINT        NOT NULL REFERENCES category_attributes(id),
    value         VARCHAR(500),
    CONSTRAINT uq_product_attribute_values UNIQUE (product_id, attribute_id)
);

CREATE INDEX idx_product_attribute_values_product ON product_attribute_values(product_id);
