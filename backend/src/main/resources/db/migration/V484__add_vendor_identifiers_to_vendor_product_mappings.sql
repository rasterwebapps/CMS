-- Inventory Management — Phase 1 kickoff item #3 ("Vendor part-number/name on
-- VendorProductMapping"): lets a mapping record the supplier's own identifier and name for the
-- product, which commonly differ from ours — needed to match a supplier's invoice/catalog line
-- back to our Product without guessing. Both optional/additive.

ALTER TABLE vendor_product_mappings
    ADD COLUMN vendor_part_number  VARCHAR(100),
    ADD COLUMN vendor_product_name VARCHAR(200);
