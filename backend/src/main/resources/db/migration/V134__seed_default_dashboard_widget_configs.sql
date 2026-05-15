-- ─────────────────────────────────────────────────────────────────────────────
-- V134 — Seed default dashboard widget configurations for all roles.
--
-- After this migration every role has an ordered, col/row-span-aware widget list
-- so the Phase 3 dynamic renderer (useDynamicRenderer) activates for all users.
--
-- Layout rules used:
--   • Every row adds up to exactly 4 grid columns — no gaps.
--   • hero  = col_span 4  (full-width welcome banner)
--   • stat cards = col_span 1  (compact KPI tiles)
--   • chart / list cards = col_span 2  (half-width)
--   • full-width sections = col_span 4
--
-- Uses NOT EXISTS guards so re-running (or applying on an environment that
-- already had some widgets configured) is safe — existing entries are kept.
-- ─────────────────────────────────────────────────────────────────────────────

-- ══ ADMIN GROUP (DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN) ═════════════
-- Row 1 : hero                              (4)
-- Row 2 : students | faculty | labs | fee   (1+1+1+1)
-- Row 3 : outstanding | enquiries           (2+2)
-- Row 4 : quick-actions                     (4)
-- Row 5 : trend-chart | pending-approvals   (2+2)
-- Row 6 : equipment-status | fee-overview   (2+2)
-- Row 7 : system-health | recent-activity   (2+2)
-- Row 8 : colleagues                        (4)

WITH admin_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('hero',              0,  4, 1),
    ('stat-students',     1,  1, 1),
    ('stat-faculty',      2,  1, 1),
    ('stat-labs',         3,  1, 1),
    ('stat-fee-collected',4,  1, 1),
    ('stat-outstanding',  5,  2, 1),
    ('stat-enquiries',    6,  2, 1),
    ('quick-actions',     7,  4, 1),
    ('chart-trend',       8,  2, 1),
    ('pending-approvals', 9,  2, 1),
    ('equipment-status',  10, 2, 1),
    ('fee-overview',      11, 2, 1),
    ('system-health',     12, 2, 1),
    ('recent-activity',   13, 2, 1),
    ('colleagues',        14, 4, 1)
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
         WHERE  c.role_id   = r.id
           AND  c.widget_key = w.widget_key
       );

-- ══ FACULTY ══════════════════════════════════════════════════════════════════
-- Row 1 : hero                              (4)
-- Row 2 : doc-stats                         (4)
-- Row 3 : completion-ring | colleagues      (2+2)
-- Row 4 : recent-activity                   (4)

WITH faculty_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('hero',              0, 4, 1),
    ('doc-stats',         1, 4, 1),
    ('completion-ring',   2, 2, 1),
    ('colleagues',        3, 2, 1),
    ('recent-activity',   4, 4, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   faculty_widgets w
CROSS  JOIN app_roles r
WHERE  r.name = 'FACULTY'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id   = r.id
           AND  c.widget_key = w.widget_key
       );

-- ══ STUDENT ══════════════════════════════════════════════════════════════════
-- Row 1 : hero                              (4)
-- Row 2 : doc-stats                         (4)
-- Row 3 : completion-ring | recent-activity (2+2)
-- Row 4 : student-quicklinks                (4)

WITH student_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('hero',                0, 4, 1),
    ('doc-stats',           1, 4, 1),
    ('completion-ring',     2, 2, 1),
    ('recent-activity',     3, 2, 1),
    ('student-quicklinks',  4, 4, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   student_widgets w
CROSS  JOIN app_roles r
WHERE  r.name = 'STUDENT'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id   = r.id
           AND  c.widget_key = w.widget_key
       );

-- ══ CASHIER ══════════════════════════════════════════════════════════════════
-- Row 1 : hero                              (4)
-- Row 2 : stat-fee-collected | outstanding  (2+2)
-- Row 3 : fee-overview                      (4)
-- Row 4 : quick-actions                     (4)

WITH cashier_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('hero',               0, 4, 1),
    ('stat-fee-collected', 1, 2, 1),
    ('stat-outstanding',   2, 2, 1),
    ('fee-overview',       3, 4, 1),
    ('quick-actions',      4, 4, 1)
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
         WHERE  c.role_id   = r.id
           AND  c.widget_key = w.widget_key
       );

-- ══ FRONT_OFFICE ═════════════════════════════════════════════════════════════
-- Row 1 : hero                              (4)
-- Row 2 : stat-students | stat-enquiries    (2+2)
-- Row 3 : pending-approvals                 (4)
-- Row 4 : quick-actions                     (4)

WITH fo_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('hero',               0, 4, 1),
    ('stat-students',      1, 2, 1),
    ('stat-enquiries',     2, 2, 1),
    ('pending-approvals',  3, 4, 1),
    ('quick-actions',      4, 4, 1)
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
         WHERE  c.role_id   = r.id
           AND  c.widget_key = w.widget_key
       );
