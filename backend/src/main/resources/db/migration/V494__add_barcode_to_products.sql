-- Inventory Management — Phase 3 item #10 ("Barcode/GTIN — capture + lookup + label printing"):
-- an optional real-world barcode/GTIN value on Product. Partial unique index (not a plain UNIQUE
-- column constraint) since most existing products have none yet and NULL/blank must never
-- collide — same pattern as uq_vendor_product_mappings_active_pair's WHERE-scoped uniqueness.

ALTER TABLE products
    ADD COLUMN barcode VARCHAR(64);

CREATE UNIQUE INDEX uq_products_barcode ON products (LOWER(barcode)) WHERE barcode IS NOT NULL;
