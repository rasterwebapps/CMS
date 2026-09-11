-- Inventory Management — wires ProductVariant into the stock-tracking core (StockBatch,
-- StockLedger, StockBalance) so a movement can be posted against a specific variant, not just its
-- parent product. Nullable throughout: existing rows and products with no variants are unaffected.
-- Once a product has any active variant, StockMovementService.recordMovement requires one on every
-- movement for that product — see the 2026-09-11 "Wire ProductVariant into Stock Movement"
-- decision-log entry. Column names verified against V426 (stock_batches/stock_ledger/
-- stock_balances) and V495 (product_variants).

ALTER TABLE stock_batches ADD COLUMN variant_id BIGINT REFERENCES product_variants(id);
ALTER TABLE stock_ledger ADD COLUMN variant_id BIGINT REFERENCES product_variants(id);
ALTER TABLE stock_balances ADD COLUMN variant_id BIGINT REFERENCES product_variants(id);

CREATE INDEX idx_stock_batches_variant ON stock_batches(variant_id);
CREATE INDEX idx_stock_ledger_variant ON stock_ledger(variant_id);
CREATE INDEX idx_stock_balances_variant ON stock_balances(variant_id);

-- Widen both uniqueness constraints to include variant_id. NULLS NOT DISTINCT so a NULL
-- variant_id still collapses to one row per (product, batch)/(product, location, batch) exactly
-- like before this migration, for products that have no variants at all.
ALTER TABLE stock_batches DROP CONSTRAINT uq_stock_batches_product_batch;
ALTER TABLE stock_batches ADD CONSTRAINT uq_stock_batches_product_batch
    UNIQUE NULLS NOT DISTINCT (product_id, variant_id, batch_or_serial_no);

ALTER TABLE stock_balances DROP CONSTRAINT uq_stock_balances;
ALTER TABLE stock_balances ADD CONSTRAINT uq_stock_balances
    UNIQUE NULLS NOT DISTINCT (product_id, variant_id, location_id, batch_id);
