-- Ordered list of dashboard widget keys configured per role.
-- Widgets are rendered in the order stored here.
CREATE TABLE role_dashboard_widgets (
    role_id      BIGINT       NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    widget_key   VARCHAR(100) NOT NULL,
    widget_order INTEGER      NOT NULL,
    PRIMARY KEY (role_id, widget_order)
);

CREATE INDEX idx_role_dashboard_widgets_role ON role_dashboard_widgets(role_id);