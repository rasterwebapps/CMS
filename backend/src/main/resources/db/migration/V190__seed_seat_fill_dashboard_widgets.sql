-- V190 — Seed 2 seat-fill stat-card widgets for admin-group roles.
--
-- New widgets:
--   stat-counselling-seats-fill  — counselling seats filled vs total (current year)
--   stat-management-seats-fill   — management seats filled vs total (current year)
--
-- Orders 36–37, continuing after V162 admission KPI widgets (orders 31–35).
-- Idempotent via NOT EXISTS guards.
-- ─────────────────────────────────────────────────────────────────────────────

WITH seat_fill_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('stat-counselling-seats-fill', 36, 1, 1),
    ('stat-management-seats-fill',  37, 1, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   seat_fill_widgets w
CROSS  JOIN app_roles r
WHERE  r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN', 'collegeadmin')
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );
