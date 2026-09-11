-- Inventory Management — Phase 3 item #11 (final item) ("ProductVariant entity — parent/child SKU
-- matrix"): a Product can have many ProductVariant rows, each its own sellable/stockable SKU (e.g.
-- "Red / Large" vs "Blue / Small" of the same base Product). "Inheriting typed attributes,
-- tracking mode, pricing, ... and barcode-per-variant from the phases above" is implemented as
-- copy-at-creation (the frontend pre-fills a new variant's form from its parent Product's current
-- values), never a live link — same "snapshot, never re-derived" posture already used for
-- ProductUomChainVersion and UomConversionTemplate. A variant's own Unit Hierarchy is not a
-- separate concept: it shares its parent Product's ProductUomChainVersion (both are keyed off
-- baseUomId, which a variant doesn't override), which is exactly what the "UOM templates ... so
-- variants don't each redefine a chain from scratch" framing already achieves with zero extra
-- schema — see docs/inventory-management/DECISION_LOG.md.

CREATE TABLE product_variants (
    id             BIGSERIAL      PRIMARY KEY,
    product_id     BIGINT         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_code   VARCHAR(50)    NOT NULL,
    variant_name   VARCHAR(200)   NOT NULL,
    barcode        VARCHAR(64),
    tracking_mode  VARCHAR(20)    NOT NULL DEFAULT 'NONE',
    standard_cost  NUMERIC(14,2),
    list_price     NUMERIC(14,2),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_variants_code UNIQUE (variant_code),
    CONSTRAINT chk_product_variants_tracking_mode CHECK (tracking_mode IN ('NONE', 'BATCH', 'SERIAL'))
);

CREATE INDEX idx_product_variants_product ON product_variants(product_id);
CREATE UNIQUE INDEX uq_product_variants_barcode ON product_variants (LOWER(barcode)) WHERE barcode IS NOT NULL;

-- Same typed-EAV shape as product_attribute_values (V424/V482) — a variant's own values for
-- whichever of its parent category's attributes are variant-defining (e.g. Size, Color), pre-
-- filled from the parent Product's current values at creation, then independently editable.
CREATE TABLE product_variant_attribute_values (
    id            BIGSERIAL     PRIMARY KEY,
    variant_id    BIGINT        NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    attribute_id  BIGINT        NOT NULL REFERENCES category_attributes(id),
    text_value    VARCHAR(500),
    number_value  NUMERIC(18,4),
    date_value    DATE,
    boolean_value BOOLEAN,
    CONSTRAINT uq_product_variant_attribute_values UNIQUE (variant_id, attribute_id)
);

CREATE INDEX idx_product_variant_attribute_values_variant ON product_variant_attribute_values(variant_id);
