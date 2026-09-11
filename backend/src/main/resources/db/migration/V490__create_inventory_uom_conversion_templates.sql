-- Inventory Management — Phase 2 item #8 ("Shared/global UOM conversion templates"): reusable
-- "1 BOX = 10 EA"-style chain definitions a product's own Unit Hierarchy (product_uom_levels) can
-- be pre-filled from instead of typed out each time. No live link once applied — the product's
-- own ProductUomChainVersion/ProductUomLevel rows are still the only source of truth for that
-- product's chain, same "snapshot, never re-derived" spirit as everything else in that slice; see
-- docs/inventory-management/DECISION_LOG.md.

CREATE TABLE uom_conversion_templates (
    id           BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(150)  NOT NULL,
    description  VARCHAR(500),
    base_uom_id  BIGINT        NOT NULL REFERENCES uoms(id),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_uom_conversion_templates_name UNIQUE (name)
);

CREATE INDEX idx_uom_conversion_templates_base_uom ON uom_conversion_templates(base_uom_id);

-- Levels are a child collection of their template, replaced wholesale on every template save —
-- same pattern as ProductAlias/ProductAttributeValue on Product.
CREATE TABLE uom_conversion_template_levels (
    id                   BIGSERIAL     PRIMARY KEY,
    template_id          BIGINT        NOT NULL REFERENCES uom_conversion_templates(id) ON DELETE CASCADE,
    uom_id               BIGINT        NOT NULL REFERENCES uoms(id),
    level_rank           INTEGER       NOT NULL,
    factor_to_base       NUMERIC(18,6) NOT NULL DEFAULT 1,
    is_default_purchase  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_uom_conversion_template_levels_template ON uom_conversion_template_levels(template_id);
