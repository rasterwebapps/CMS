-- Inventory Management — Phase 1 kickoff item #1 ("Typed EAV storage"): product_attribute_values
-- moves from a single free-text `value` column to four type-specific columns, one per
-- CategoryAttribute.data_type, so a value is stored (and can be queried/sorted) as its real type
-- instead of as text the application layer re-parses on every read. See
-- docs/inventory-management/DECISION_LOG.md for the full rationale.

ALTER TABLE product_attribute_values
    ADD COLUMN text_value    VARCHAR(500),
    ADD COLUMN number_value  NUMERIC(18,4),
    ADD COLUMN date_value    DATE,
    ADD COLUMN boolean_value BOOLEAN;

-- Backfill any existing rows into the column matching their attribute's declared data type.
UPDATE product_attribute_values pav
SET number_value = pav.value::numeric
FROM category_attributes ca
WHERE ca.id = pav.attribute_id AND ca.data_type = 'NUMBER' AND pav.value IS NOT NULL;

UPDATE product_attribute_values pav
SET date_value = pav.value::date
FROM category_attributes ca
WHERE ca.id = pav.attribute_id AND ca.data_type = 'DATE' AND pav.value IS NOT NULL;

UPDATE product_attribute_values pav
SET boolean_value = (pav.value = 'true')
FROM category_attributes ca
WHERE ca.id = pav.attribute_id AND ca.data_type = 'BOOLEAN' AND pav.value IS NOT NULL;

UPDATE product_attribute_values pav
SET text_value = pav.value
FROM category_attributes ca
WHERE ca.id = pav.attribute_id AND ca.data_type IN ('TEXT', 'ENUM') AND pav.value IS NOT NULL;

ALTER TABLE product_attribute_values DROP COLUMN value;
