-- ─────────────────────────────────────────────────────────────────────────────
-- V145 — Seed the four new admin-analytics widgets into the default layout
--        of every admin-class role (DEV_ADMIN, SUPPORT_ADMIN, ADMIN,
--        COLLEGE_ADMIN).
--
-- Widgets added (each col_span = 2, half-width):
--   • admission-funnel        — pipeline stages with conversion %
--   • fee-collection-target   — collected vs target gauge for current month
--   • dues-aging              — outstanding fees grouped by days overdue
--   • program-admissions      — enrolled student count per program
--
-- Orders 15–18 keep them at the end of the existing admin layout (V134 used
-- 0–14). Re-running is safe — NOT EXISTS guard skips rows that are already
-- present (e.g. for installations that ran the seed via the admin UI first).
-- ─────────────────────────────────────────────────────────────────────────────

WITH new_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('admission-funnel',      15, 2, 1),
    ('fee-collection-target', 16, 2, 1),
    ('dues-aging',            17, 2, 1),
    ('program-admissions',    18, 2, 1)
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

-- Cashier also benefits from the fee-related ones.
WITH cashier_extras (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('fee-collection-target', 5, 2, 1),
    ('dues-aging',            6, 2, 1)
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

-- Front Office benefits from the funnel.
WITH fo_extras (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('admission-funnel',  5, 4, 1)
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

