-- Inventory Management — wires ProductVariant into Purchase Order lines. Nullable: existing lines
-- and products with no variants are unaffected. Once a product has any active variant,
-- PurchaseOrderService.addLine requires one on every new line ordering that product — same rule
-- as Stock Movement (V497). Goods Receipt lines don't get their own column: a receipt line always
-- inherits its variant from the purchase_order_item it's receiving against (see
-- GoodsReceiptService/SupplierReturnService). Column names verified against V438
-- (purchase_order_items) and V495 (product_variants).

ALTER TABLE purchase_order_items ADD COLUMN variant_id BIGINT REFERENCES product_variants(id);

CREATE INDEX idx_purchase_order_items_variant ON purchase_order_items(variant_id);
