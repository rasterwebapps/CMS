-- Reverting a PUBLISHED (live) timetable back to Draft is materially more destructive than
-- discarding an unapproved DRAFT (TIMETABLE_MANAGE already covers that) -- it affects a timetable
-- students/faculty may already be relying on. Kept as its own permission per the operation-wise
-- permission mapping gate rather than folding into TIMETABLE_MANAGE.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_DISCARD_PUBLISHED', 'Revert Published Timetable to Draft', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant to roles that already hold TIMETABLE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_DISCARD_PUBLISHED') new_p
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
