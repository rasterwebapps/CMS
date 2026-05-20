-- V162 — Seed 5 admission KPI stat-card widgets for admin-group roles.
--
-- New widgets:
--   stat-male-students      — total male students
--   stat-female-students    — total female students
--   stat-management-quota   — students admitted via management quota
--   stat-counselling-quota  — students admitted via counselling
--   stat-govt-lapsed-seats  — government quota seats that went unfilled
--
-- Orders 31–35 keep them after all existing Tier-1/2/3 widgets.
-- Idempotent via NOT EXISTS guards.
-- ─────────────────────────────────────────────────────────────────────────────

WITH admission_kpi_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('stat-male-students',     31, 1, 1),
    ('stat-female-students',   32, 1, 1),
    ('stat-management-quota',  33, 1, 1),
    ('stat-counselling-quota', 34, 1, 1),
    ('stat-govt-lapsed-seats', 35, 1, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   admission_kpi_widgets w
CROSS  JOIN app_roles r
WHERE  r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN', 'collegeadmin')
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );
