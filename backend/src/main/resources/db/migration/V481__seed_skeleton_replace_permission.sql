-- Permission for the new "Replace" action on a placed Theory session (Skeleton Builder grid).
--
-- Distinct from TIMETABLE_SKELETON_MANAGE per the operation-wise permission mapping rule. Replacing
-- is not simply a place plus a remove: it hands a slot from one subject to another, which puts the
-- DISPLACED subject below its curriculum-hours requirement somewhere else in the term. That is a
-- quieter and further-reaching consequence than placing or removing a single session, so it gets
-- its own permission rather than riding on MANAGE.
--
-- Defaults to whichever role already holds TIMETABLE_SKELETON_MOVE -- the closest existing tier,
-- since both are "reshape a draft grid that already exists" capabilities, as opposed to MANAGE's
-- broader create/delete rights.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_REPLACE', 'Replace Timetable Skeleton Subject', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MOVE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_REPLACE') new_p
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
