-- Inventory Management — Phase 2 item #7 ("HSN/SAC + default TaxRule on Product"): a
-- classification code and a default tax rate a Purchase Order line can pre-fill from (the line
-- keeps its own taxRuleId as an override — see PurchaseOrderService.addLine).
--
-- default_tax_rule_id is a real DB-level FK (referential integrity is free and meaningful here —
-- unlike RequisitionLineItem.ChargeableCostObjectId, this always points at exactly one table) but
-- deliberately carried as a bare id, never a JPA relationship, on the Product entity itself: the
-- ER_DIAGRAM_AND_MODULE_BOUNDARIES.md dependency graph has CATALOG --> PROC (Procurement depends
-- on Catalog), and TaxRule lives in the procurement package — a `@ManyToOne` from Product to
-- TaxRule would invert that into a cycle. PurchaseOrderService (which already depends on Catalog)
-- resolves the id when it needs the actual TaxRule. See docs/inventory-management/DECISION_LOG.md.

ALTER TABLE products
    ADD COLUMN hsn_sac_code       VARCHAR(20),
    ADD COLUMN default_tax_rule_id BIGINT REFERENCES tax_rules(id);
