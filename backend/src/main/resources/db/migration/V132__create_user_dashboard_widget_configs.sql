-- Per-user dashboard layout overrides.
-- When a user has rows here the system uses their personal config instead of the role default.
-- Deleting all rows for a user resets them back to the role default.

CREATE TABLE user_dashboard_widget_configs (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    widget_key   VARCHAR(100) NOT NULL,
    widget_order INTEGER      NOT NULL DEFAULT 0,
    col_span     SMALLINT     NOT NULL DEFAULT 1
                              CHECK (col_span BETWEEN 1 AND 4),
    row_span     SMALLINT     NOT NULL DEFAULT 1
                              CHECK (row_span BETWEEN 1 AND 2),
    config_json  JSONB,
    CONSTRAINT uq_user_widget UNIQUE (user_id, widget_key)
);

CREATE INDEX idx_udwc_user_order ON user_dashboard_widget_configs(user_id, widget_order);
