-- Inventory Management (Release 3) — Phase 5 "Equipment & Asset Management" fourth and final
-- slice: Disposal/write-off. Adds disposal reason/value/date/actor directly to the already-
-- shipped assets table (V452) rather than a new child table — disposal is a single terminal
-- action on the asset itself, not its own multi-line document. New forward migration on an
-- already-shipped table; V452 itself untouched. See docs/inventory-management/DECISION_LOG.md's
-- "Disposal slice" entry.

ALTER TABLE assets ADD COLUMN disposal_reason VARCHAR(500);
ALTER TABLE assets ADD COLUMN disposal_value NUMERIC(14,2);
ALTER TABLE assets ADD COLUMN disposal_date DATE;
ALTER TABLE assets ADD COLUMN disposed_by VARCHAR(255);
ALTER TABLE assets ADD COLUMN disposed_at TIMESTAMPTZ;
