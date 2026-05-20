-- ─────────────────────────────────────────────────────────────────────────────
-- V148 — Tier-4 exception / alert widgets.
--
-- Adds the minimal schema needed for passive strategic alerts:
--   • programs.seat_capacity for capacity warnings
--   • compliance_documents for UGC/NAAC/AICTE/university expiry alerts
--
-- Also seeds configurable alert widgets into admin-class role dashboards.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE programs
  ADD COLUMN IF NOT EXISTS seat_capacity INTEGER;

CREATE TABLE IF NOT EXISTS compliance_documents (
  id               BIGSERIAL PRIMARY KEY,
  authority        VARCHAR(50)  NOT NULL,
  document_name    VARCHAR(200) NOT NULL,
  reference_number VARCHAR(100),
  expires_on       DATE,
  status           VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
  remarks          TEXT,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_compliance_documents_expiry
  ON compliance_documents (expires_on);

CREATE INDEX IF NOT EXISTS idx_compliance_documents_authority
  ON compliance_documents (authority);

WITH admin_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('anomaly-banner',     31, 4, 1),
    ('capacity-alert',     32, 2, 1),
    ('compliance-alerts',  33, 2, 1),
    ('audit-mini-feed',    34, 4, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   admin_widgets w
CROSS  JOIN app_roles r
WHERE  r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

-- Cashier: collection anomaly is a finance-critical passive alert.
WITH cashier_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('anomaly-banner', 11, 4, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   cashier_widgets w
CROSS  JOIN app_roles r
WHERE  r.name = 'CASHIER'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

-- Front office: capacity pressure is useful during admissions.
WITH fo_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('capacity-alert', 11, 4, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   fo_widgets w
CROSS  JOIN app_roles r
WHERE  r.name = 'FRONT_OFFICE'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

