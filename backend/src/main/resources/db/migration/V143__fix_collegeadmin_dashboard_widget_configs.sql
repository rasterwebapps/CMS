-- ─────────────────────────────────────────────────────────────────────────────
-- V143 — Fix dashboard widget configs for the 'collegeadmin' role.
--
-- Root cause: V134 seeded widgets for the role named 'COLLEGE_ADMIN' (uppercase),
-- but V123 created the role as 'collegeadmin' (lowercase, no underscore).
-- V125 then deleted the uppercase 'COLLEGE_ADMIN' row, leaving 'collegeadmin'
-- with zero widget configs. The frontend fell back to its hardcoded DEFAULT_WIDGET_KEYS
-- list — which is why widgets appeared to load without any DB-driven config.
--
-- This migration seeds the same admin-group widget layout for 'collegeadmin' so
-- the dynamic renderer uses the DB-configured layout, not the frontend fallback.
--
-- Layout (matches the admin group in V134):
--   Row 1 : hero                              (4)
--   Row 2 : students | faculty | labs | fee   (1+1+1+1)
--   Row 3 : outstanding | enquiries           (2+2)
--   Row 4 : quick-actions                     (4)
--   Row 5 : trend-chart | pending-approvals   (2+2)
--   Row 6 : equipment-status | fee-overview   (2+2)
--   Row 7 : system-health | recent-activity   (2+2)
--   Row 8 : colleagues                        (4)
--
-- NOT EXISTS guards make this safe to re-run.
-- ─────────────────────────────────────────────────────────────────────────────

WITH college_admin_widgets (widget_key, widget_order, col_span, row_span) AS (
  VALUES
    ('hero',               0,  4, 1),
    ('stat-students',      1,  1, 1),
    ('stat-faculty',       2,  1, 1),
    ('stat-labs',          3,  1, 1),
    ('stat-fee-collected', 4,  1, 1),
    ('stat-outstanding',   5,  2, 1),
    ('stat-enquiries',     6,  2, 1),
    ('quick-actions',      7,  4, 1),
    ('chart-trend',        8,  2, 1),
    ('pending-approvals',  9,  2, 1),
    ('equipment-status',   10, 2, 1),
    ('fee-overview',       11, 2, 1),
    ('system-health',      12, 2, 1),
    ('recent-activity',    13, 2, 1),
    ('colleagues',         14, 4, 1)
)
INSERT INTO role_dashboard_widget_configs
      (role_id, widget_key, widget_order, col_span, row_span)
SELECT r.id, w.widget_key, w.widget_order, w.col_span, w.row_span
FROM   college_admin_widgets w
CROSS  JOIN app_roles r
WHERE  r.name = 'collegeadmin'
  AND  NOT EXISTS (
         SELECT 1
         FROM   role_dashboard_widget_configs c
         WHERE  c.role_id    = r.id
           AND  c.widget_key = w.widget_key
       );

