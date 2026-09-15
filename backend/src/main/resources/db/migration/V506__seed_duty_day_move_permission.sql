-- 2026-09-15: permission for dragging a cohort's Clinical duty banner to another day in Skeleton
-- Builder. The duty's new day must pass every check Run Automation applies, and that day's sessions
-- inside the duty window swap into the day the duty leaves. It is its own permission per the
-- operation-wise permission mapping rule -- not TIMETABLE_SKELETON_MOVE, which moves class sessions
-- -- because it changes which day real students are off campus on hospital duty all term.
--
-- Defaults to whichever role already holds TIMETABLE_SKELETON_MOVE: the banner drag sits on the same
-- grid as moving sessions, so that is the closest existing tier. Wider/narrower access is then
-- handled in Role Management.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_DUTY_DAY_MOVE', 'Move Clinical Duty Day', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MOVE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_DUTY_DAY_MOVE') new_p
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
