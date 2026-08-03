-- "Generate Blueprint" is a real mutating admin action (freezes/replaces planned completion
-- dates for a whole offering), distinct from the existing read-only PROGRESS_REPORT_VIEW /
-- PROGRESS_LOG_CREATE permissions -- gets its own dedicated permission per the per-operation rule.
-- Default tier copied from TIMETABLE_SKELETON_MANAGE, the closest existing equivalent (an admin
-- mutating action on the same timetable domain). Reading the blueprint/projection/shortfall reuses
-- the existing PROGRESS_REPORT_VIEW permission -- no new view permission needed.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('PORTION_BLUEPRINT_MANAGE', 'Generate Portion-Completion Blueprint', 'CURRICULUM', 'Progress Report', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'PORTION_BLUEPRINT_MANAGE') new_p
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
