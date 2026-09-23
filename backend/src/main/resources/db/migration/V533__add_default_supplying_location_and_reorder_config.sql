-- Inventory Management — Phase B of the Stock Indent auto-indent feature (see
-- docs/inventory-management/DECISION_LOG.md's 2026-09-21 "OC-206 reopened" entry). Two pieces:
--
-- 1. `default_supplying_location_id` on inventory_locations — the store a requesting-point
--    location normally draws from, set explicitly per location rather than inferred from the
--    campus hierarchy (decision #3). Self-referential, nullable (most locations won't set it
--    until this feature is actually configured), no ON DELETE action needed since a location is
--    never hard-deleted while referenced (matches how every other InventoryLocation FK in this
--    schema is left to the default RESTRICT).
--
-- 2. `product_location_reorder_configs` — per-(product, location) reorder level/qty/max stock
--    and an auto-indent flag (decision #2: per-location, not the existing global
--    Product.reorder_level/reorder_qty, which keeps feeding the separate Wanted List). One active
--    config per (product, location) pair, same partial-unique-index shape as
--    vendor_product_mappings (V432) — a deactivated config doesn't block a new one.
--
-- Column names verified against V426 (inventory_locations) and V424/V489 (products) — this file's
-- own migration number (V533) follows V532, the last one shipped.

ALTER TABLE inventory_locations
    ADD COLUMN default_supplying_location_id BIGINT REFERENCES inventory_locations(id);

CREATE TABLE product_location_reorder_configs (
    id                    BIGSERIAL     PRIMARY KEY,
    product_id            BIGINT        NOT NULL REFERENCES products(id),
    location_id           BIGINT        NOT NULL REFERENCES inventory_locations(id),
    reorder_level         NUMERIC(14,3) NOT NULL,
    reorder_qty           NUMERIC(14,3) NOT NULL,
    max_stock_qty         NUMERIC(14,3),
    auto_indent_enabled   BOOLEAN       NOT NULL DEFAULT TRUE,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_product_location_reorder_configs_reorder_level CHECK (reorder_level >= 0),
    CONSTRAINT chk_product_location_reorder_configs_reorder_qty CHECK (reorder_qty > 0),
    CONSTRAINT chk_product_location_reorder_configs_max_stock_qty
        CHECK (max_stock_qty IS NULL OR max_stock_qty >= reorder_level)
);

CREATE UNIQUE INDEX uq_product_location_reorder_configs_active_pair
    ON product_location_reorder_configs (product_id, location_id)
    WHERE is_active;

CREATE INDEX idx_product_location_reorder_configs_location ON product_location_reorder_configs(location_id);
CREATE INDEX idx_product_location_reorder_configs_product ON product_location_reorder_configs(product_id);
