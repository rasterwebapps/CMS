-- Inventory Management — Phase 1 kickoff item #2 ("Serial/batch tracking-mode flag"): replaces
-- the previously-ambiguous free-text StockBatch.batchOrSerialNo with an explicit per-product
-- declaration of how its stock is tracked. NONE is the default and reproduces today's behavior
-- (batchOrSerialNo stays optional free text) so no existing product or movement changes meaning
-- on this migration alone — see docs/inventory-management/DECISION_LOG.md.

ALTER TABLE products
    ADD COLUMN tracking_mode VARCHAR(20) NOT NULL DEFAULT 'NONE';

ALTER TABLE products
    ADD CONSTRAINT chk_products_tracking_mode CHECK (tracking_mode IN ('NONE', 'BATCH', 'SERIAL'));
