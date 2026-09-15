-- OC-227: permission for the Global Auto-Schedule run report's "Apply duty length" button.
--
-- A 6h Clinical Shift over a 26-week term delivers 156h against a 160h curriculum unit, so almost
-- every shift-configured subject ends a few hours short. The run report now proposes the minimum
-- duty length that closes it (e.g. 6h -> 6h10m) and flags when that costs zero timetable periods;
-- this button applies it to the Course Offering's clinical shift duration. It is its own permission
-- per the operation-wise permission mapping rule -- not COURSE_MANAGE (the offering edit screen's
-- permission) and not TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE (running automation) -- because it
-- changes how long real students are on hospital duty every week of the term.
--
-- Defaults to whichever role already holds TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE: the button only
-- ever appears in that run's report, so that is the closest existing tier. Wider/narrower access is
-- then handled in Role Management.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_CLINICAL_DUTY_FIT', 'Apply Clinical Duty-Length Fit', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_CLINICAL_DUTY_FIT') new_p
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
