-- ─────────────────────────────────────────────────────────────────────────────
-- V146 — Seed the four Tier-2 analytics widgets into the default layout of
--        the appropriate roles.
--
-- Widgets added (each col_span = 2, half-width):
--   • agent-performance         — leaderboard of top referral agents
--   • program-revenue-mix       — donut of net revenue per program
--   • scholarship-burn          — gross→discount/scholarship→net stacked bar
--   • doc-verification-backlog  — pending verification counter with CTA
--
-- Orders 19–22 keep them after the Tier-1 widgets (V145 used 15–18).
-- Idempotent via NOT EXISTS guard.
-- ─────────────────────────────────────────────────────────────────────────────

WITH new_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('agent-performance',         19, 2, 1),
    ('program-revenue-mix',       20, 2, 1),
    ('scholarship-burn',          21, 2, 1),
    ('doc-verification-backlog',  22, 2, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   new_widgets w
CROSS  JOIN app_roles r
WHERE  r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

-- CASHIER also benefits from the burn breakdown.
WITH cashier_extras (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('scholarship-burn',          7, 2, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   cashier_extras w
CROSS  JOIN app_roles r
WHERE  r.name = 'CASHIER'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

-- FRONT_OFFICE benefits from agent perf + the verification backlog.
WITH fo_extras (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('agent-performance',         6, 2, 1),
    ('doc-verification-backlog',  7, 2, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   fo_extras w
CROSS  JOIN app_roles r
WHERE  r.name = 'FRONT_OFFICE'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

