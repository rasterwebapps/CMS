-- Permission for the new Skeleton Builder drag-and-drop move (R3 Step 5) — distinct from
-- TIMETABLE_SKELETON_MANAGE (place/remove) per the operation-wise permission mapping rule: every
-- distinct operation gets its own permission. Auto-assigned to any role that already holds
-- TIMETABLE_SKELETON_MANAGE (closest-match tier).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_MOVE', 'Move Timetable Skeleton Cells', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_MOVE') new_p
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
