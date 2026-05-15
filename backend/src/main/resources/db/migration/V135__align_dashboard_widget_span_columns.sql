-- Align dashboard widget span columns with the Java entity fields.
-- The entities and DTOs expose colSpan/rowSpan as Integer/int values; PostgreSQL
-- must use INTEGER so Hibernate schema validation does not fail on SMALLINT/int2.
ALTER TABLE role_dashboard_widget_configs
    ALTER COLUMN col_span TYPE INTEGER,
    ALTER COLUMN row_span TYPE INTEGER;
ALTER TABLE user_dashboard_widget_configs
    ALTER COLUMN col_span TYPE INTEGER,
    ALTER COLUMN row_span TYPE INTEGER;
