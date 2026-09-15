-- Phase 2 of OC-229 (see V507's header comment): a splittable breakdown of a StockBalance row's
-- quantity across InventoryBins. StockBalance/StockLedger stay keyed on (product, variant,
-- location, batch) as the source of truth for available quantity — this table is a pure locator
-- breakdown that must sum to its parent stock_balances.qty_on_hand, mirroring the same
-- "ledger vs. derived balance" pattern V426 already established (stock_ledger -> stock_balances).
-- FKs to stock_balances(id) directly (not the raw product/variant/location/batch columns) since
-- that row already carries the NULLS NOT DISTINCT composite-key semantics (V497) — no need to
-- re-derive them here. Column names verified against V426/V497 (stock_balances) and V507
-- (inventory_bins).

CREATE TABLE stock_bin_allocations (
    id                BIGSERIAL      PRIMARY KEY,
    stock_balance_id  BIGINT         NOT NULL REFERENCES stock_balances(id),
    bin_id            BIGINT         NOT NULL REFERENCES inventory_bins(id),
    qty               NUMERIC(14,3)  NOT NULL DEFAULT 0,
    last_updated      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_bin_allocations UNIQUE (stock_balance_id, bin_id)
);

CREATE INDEX idx_stock_bin_allocations_balance ON stock_bin_allocations(stock_balance_id);
CREATE INDEX idx_stock_bin_allocations_bin ON stock_bin_allocations(bin_id);
