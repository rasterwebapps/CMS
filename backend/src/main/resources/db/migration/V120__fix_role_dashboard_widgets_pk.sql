-- V119 created role_dashboard_widgets with PRIMARY KEY (role_id, widget_order).
-- Hibernate 6 @OrderColumn issues UPDATE widget_order statements when reordering,
-- which fail on a PK column. Drop and recreate without the composite PK so
-- Hibernate can freely issue DELETE / INSERT / UPDATE against the table.
DROP TABLE IF EXISTS role_dashboard_widgets;

CREATE TABLE role_dashboard_widgets (
    role_id      BIGINT       NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    widget_key   VARCHAR(100) NOT NULL,
    widget_order INTEGER      NOT NULL
);

CREATE INDEX idx_role_dashboard_widgets_role ON role_dashboard_widgets(role_id);