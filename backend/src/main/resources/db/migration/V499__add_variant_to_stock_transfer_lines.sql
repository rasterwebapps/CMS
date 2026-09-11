-- Inventory Management — wires ProductVariant into Stock Transfer lines. Nullable: existing lines
-- and products with no variants are unaffected. Once a product has any active variant,
-- StockTransferService.addLine requires one on every new line for that product — same rule as
-- Stock Movement (V497). Widens the per-transfer product uniqueness to (transfer, product,
-- variant) so distinct variants of the same product can each get their own line. Column names
-- verified against V442 (stock_transfer_lines) and V495 (product_variants).

ALTER TABLE stock_transfer_lines ADD COLUMN variant_id BIGINT REFERENCES product_variants(id);

CREATE INDEX idx_stock_transfer_lines_variant ON stock_transfer_lines(variant_id);

ALTER TABLE stock_transfer_lines DROP CONSTRAINT uq_stock_transfer_lines_product;
ALTER TABLE stock_transfer_lines ADD CONSTRAINT uq_stock_transfer_lines_product
    UNIQUE NULLS NOT DISTINCT (stock_transfer_id, product_id, variant_id);
