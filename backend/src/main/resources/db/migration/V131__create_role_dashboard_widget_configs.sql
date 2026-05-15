-- Replace the simple role_dashboard_widgets string-list table with a
-- metadata-rich configuration table that stores span and per-widget JSON config.
-- Existing widget key + order data is migrated in-place; new columns default safely.

CREATE TABLE role_dashboard_widget_configs (
    id           BIGSERIAL    PRIMARY KEY,
    role_id      BIGINT       NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    widget_key   VARCHAR(100) NOT NULL,
    widget_order INTEGER      NOT NULL DEFAULT 0,
    col_span     SMALLINT     NOT NULL DEFAULT 1
                              CHECK (col_span BETWEEN 1 AND 4),
    row_span     SMALLINT     NOT NULL DEFAULT 1
                              CHECK (row_span BETWEEN 1 AND 2),
    config_json  JSONB,
    CONSTRAINT uq_role_widget UNIQUE (role_id, widget_key)
);

CREATE INDEX idx_rdwc_role_order ON role_dashboard_widget_configs(role_id, widget_order);

-- Migrate all existing widget rows; new metadata columns take their defaults
INSERT INTO role_dashboard_widget_configs (role_id, widget_key, widget_order, col_span, row_span)
SELECT role_id, widget_key, widget_order, 1, 1
FROM   role_dashboard_widgets;

DROP TABLE role_dashboard_widgets;
