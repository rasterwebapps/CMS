-- Permission for the new Skeleton Builder "All cohorts" global multi-cohort auto-scheduler action
-- — distinct from TIMETABLE_SKELETON_AUTO_PLACE/TIMETABLE_STAFFING_AUTO_STAFF (per-cohort/term-wide
-- fill-the-gaps tools) per the operation-wise permission mapping rule: this one places AND staffs
-- every cohort in a term at once, treating CourseOffering.facultyId as authoritative. Auto-assigned
-- to any role that already holds TIMETABLE_SKELETON_MANAGE (closest-match tier, mirroring V372's
-- own precedent of granting off the corresponding MANAGE permission, not off a sibling AUTO_* one).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE', 'Global Multi-Cohort Auto-Schedule', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE') new_p
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
