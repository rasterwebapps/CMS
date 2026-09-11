-- Inventory Management — wires ProductVariant into Stock Issue Request lines. Nullable: existing
-- lines and products with no variants are unaffected. Once a product has any active variant,
-- StockIssueRequestService.addLine requires one on every new line for that product — same rule as
-- Stock Movement (V497). Widens the per-request product uniqueness to (request, product, variant)
-- so distinct variants of the same product can each get their own line. Column names verified
-- against V446 (stock_issue_request_items) and V495 (product_variants).

ALTER TABLE stock_issue_request_items ADD COLUMN variant_id BIGINT REFERENCES product_variants(id);

CREATE INDEX idx_stock_issue_request_items_variant ON stock_issue_request_items(variant_id);

ALTER TABLE stock_issue_request_items DROP CONSTRAINT uq_stock_issue_request_items_product;
ALTER TABLE stock_issue_request_items ADD CONSTRAINT uq_stock_issue_request_items_product
    UNIQUE NULLS NOT DISTINCT (stock_issue_request_id, product_id, variant_id);
