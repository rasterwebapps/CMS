-- Permissions for the new Skeleton Builder "Auto-place remaining" and Staffing "Auto-staff
-- remaining" actions (R3 Step 6) — distinct from TIMETABLE_SKELETON_MANAGE / TIMETABLE_STAFFING_MANAGE
-- per the operation-wise permission mapping rule. Auto-assigned to any role that already holds
-- the corresponding manage permission (closest-match tier).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_AUTO_PLACE', 'Auto-Place Timetable Skeleton Cells', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_STAFFING_AUTO_STAFF', 'Auto-Staff Timetable Skeleton Cells', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_AUTO_PLACE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_STAFFING_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_STAFFING_AUTO_STAFF') new_p
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
