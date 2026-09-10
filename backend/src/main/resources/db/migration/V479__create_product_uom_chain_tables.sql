-- Inventory Management — Unit-of-Measure Hierarchy slice. A Product can be purchased/received in
-- a chain of units above its base unit (e.g. Tablet -> Strip x10 -> Box x100 -> Carton x1000, or
-- ml -> Bottle x1000), each level storing its own factor directly to the base unit (per-product,
-- not a shared template — pack sizes genuinely differ item to item). The chain is versioned: a
-- pack-size change never edits an existing version's levels, it creates a new version (or
-- reactivates a matching prior one, e.g. a repack reverting from 15s back to 10s) and flips which
-- version is active. Every StockBatch permanently records which version was active when it was
-- received, so historical batches keep resolving through the pack sizes that were true at the
-- time even after the product's active version later changes. Column names verified against
-- V424's products/uoms and V426's stock_batches, and against V438's purchase_order_items /
-- V440's goods_receipt_lines (both confirmed to hold only ordered_qty/received_qty with no unit
-- column today). Design + specialist round: docs/inventory-management/DECISION_LOG.md's
-- "Unit-of-Measure Hierarchy slice" entry (2026-09-10).

CREATE TABLE product_uom_chain_versions (
    id          BIGSERIAL     PRIMARY KEY,
    product_id  BIGINT        NOT NULL REFERENCES products(id),
    version_no  INTEGER       NOT NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by  VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_uom_chain_versions_product_version UNIQUE (product_id, version_no)
);

-- Only one active version per product at a time.
CREATE UNIQUE INDEX uq_product_uom_chain_versions_active ON product_uom_chain_versions(product_id) WHERE is_active;
CREATE INDEX idx_product_uom_chain_versions_product ON product_uom_chain_versions(product_id);

CREATE TABLE product_uom_levels (
    id                    BIGSERIAL      PRIMARY KEY,
    chain_version_id      BIGINT         NOT NULL REFERENCES product_uom_chain_versions(id) ON DELETE CASCADE,
    uom_id                BIGINT         NOT NULL REFERENCES uoms(id),
    level_rank            INTEGER        NOT NULL,
    -- Direct multiplier to the chain's base level (level_rank = 0, factor_to_base = 1), decided
    -- over the chained-multiply alternative so correcting one level never silently shifts the
    -- levels above it.
    factor_to_base        NUMERIC(18,6)  NOT NULL DEFAULT 1,
    is_default_purchase   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_uom_levels_version_uom UNIQUE (chain_version_id, uom_id),
    CONSTRAINT uq_product_uom_levels_version_rank UNIQUE (chain_version_id, level_rank),
    CONSTRAINT chk_product_uom_levels_factor CHECK (factor_to_base > 0)
);

CREATE INDEX idx_product_uom_levels_chain_version ON product_uom_levels(chain_version_id);

-- Every batch remembers which chain version was active when it was received.
ALTER TABLE stock_batches ADD COLUMN chain_version_id BIGINT REFERENCES product_uom_chain_versions(id);
CREATE INDEX idx_stock_batches_chain_version ON stock_batches(chain_version_id);

-- ordered_qty/received_qty stay base-unit quantities exactly as before (every existing comparison
-- in PurchaseOrderService/GoodsReceiptService keeps working unchanged) — uom_level_id + entered_qty
-- are purely the as-typed unit/quantity kept alongside for display and audit, nullable so existing
-- rows (always base-unit, no chosen level) read back exactly as they did before this migration.
ALTER TABLE purchase_order_items ADD COLUMN uom_level_id BIGINT REFERENCES product_uom_levels(id);
ALTER TABLE purchase_order_items ADD COLUMN entered_qty NUMERIC(14,3);

ALTER TABLE goods_receipt_lines ADD COLUMN uom_level_id BIGINT REFERENCES product_uom_levels(id);
ALTER TABLE goods_receipt_lines ADD COLUMN entered_qty NUMERIC(14,3);
