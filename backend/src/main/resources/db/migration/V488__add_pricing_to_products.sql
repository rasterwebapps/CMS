-- Inventory Management — Phase 2 kickoff item #6 ("Product-level pricing"): a baseline
-- standard cost and list price (MRP) on the product itself, independent of any one supplier's
-- negotiated rate (that stays VendorProductMapping.unitPrice/effectivePrice — unchanged). Additive,
-- unconstrained columns, same shape as the Phase 1 dimensions/weight fields (V487).

ALTER TABLE products
    ADD COLUMN standard_cost NUMERIC(14,2),
    ADD COLUMN list_price    NUMERIC(14,2);
