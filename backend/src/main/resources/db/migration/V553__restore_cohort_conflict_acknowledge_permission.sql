-- V550 deleted TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE on the assumption that retiring the
-- Conflict Inspector screen left it with no consumer. That was wrong: TimetableController's
-- per-cohort acknowledge-conflicts endpoint (Timetable Builder's "Check & Resolve Conflicts" row
-- action, OC-260) still requires this exact permission code, so every user -- including
-- DEV_ADMIN -- was locked out of that action with "Access denied" once the permission row was
-- removed (role_permissions.permission_id has ON DELETE CASCADE, but the code never stopped
-- checking for it). Restoring the permission, not renaming the check, since renaming would just
-- move the same bug to whichever new code is picked.
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE', 'Acknowledge Timetable Conflict Inspector', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Re-grant to whichever roles hold TIMETABLE_MANAGE, matching V528's original defaulting rule.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
