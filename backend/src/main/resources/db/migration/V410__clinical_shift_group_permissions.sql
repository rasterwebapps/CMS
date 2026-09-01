-- OC-175/OC-177: dedicated VIEW/MANAGE permissions for Clinical Shift Groups (never shared, per
-- the operation-wise permission mapping convention). Closest-match initial tier follows the same
-- precedent as V363's Batch Rotation permissions -- MANAGE mirrors TIMETABLE_SKELETON_MANAGE
-- (this is a Skeleton-Builder-adjacent scheduling extension), VIEW mirrors TIMETABLE_VIEW.
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_CLINICAL_SHIFT_VIEW', 'View Clinical Shift Groups', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_CLINICAL_SHIFT_MANAGE', 'Manage Clinical Shift Groups', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CLINICAL_SHIFT_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CLINICAL_SHIFT_MANAGE') new_p
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
