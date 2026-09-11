-- Inventory Management — Phase 1 kickoff item #5 ("Dimensions/weight fields on Product"):
-- plain additive numeric fields, same unconstrained shape as the existing depreciationRate/
-- warrantyPeriodMonths columns (no CHECK constraints on those either) — unit is implied by the
-- column name (centimeters, kilograms), matching how warrantyPeriodMonths implies months.

ALTER TABLE products
    ADD COLUMN length_cm NUMERIC(10,2),
    ADD COLUMN width_cm  NUMERIC(10,2),
    ADD COLUMN height_cm NUMERIC(10,2),
    ADD COLUMN weight_kg NUMERIC(10,3);
