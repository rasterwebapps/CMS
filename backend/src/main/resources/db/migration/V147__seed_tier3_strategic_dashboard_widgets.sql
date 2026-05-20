-- ─────────────────────────────────────────────────────────────────────────────
-- V147 — Seed Tier-3 strategic/monthly dashboard widgets.
--
-- Widgets:
--   • geographic-admissions       — district/state recruitment heatmap grid
--   • yoy-admissions              — current year vs last two years by month
--   • refund-cancellation-rate    — refund and withdrawal KPI + trend
--   • payment-mode-breakdown      — collection mix by payment mode
--   • student-faculty-ratio       — department compliance ratio
--   • lab-utilization-heatmap     — day × slot lab schedule density
--   • cohort-retention            — term-wise cohort retention
--   • top-line-kpis               — compact daily executive KPI strip
--
-- Orders 23–30 keep them after Tier 1 (V145) and Tier 2 (V146).
-- Idempotent via NOT EXISTS guards.
-- ─────────────────────────────────────────────────────────────────────────────

WITH admin_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('top-line-kpis',              23, 4, 1),
    ('geographic-admissions',      24, 2, 1),
    ('yoy-admissions',             25, 2, 1),
    ('refund-cancellation-rate',   26, 2, 1),
    ('payment-mode-breakdown',     27, 2, 1),
    ('student-faculty-ratio',      28, 2, 1),
    ('lab-utilization-heatmap',    29, 2, 1),
    ('cohort-retention',           30, 2, 1)
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

-- Cashier: strategic finance/reconciliation widgets.
WITH cashier_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('top-line-kpis',              8, 4, 1),
    ('refund-cancellation-rate',   9, 2, 1),
    ('payment-mode-breakdown',    10, 2, 1)
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

-- Front office: admissions and geography widgets.
WITH fo_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('top-line-kpis',              8, 4, 1),
    ('geographic-admissions',      9, 2, 1),
    ('yoy-admissions',            10, 2, 1)
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

