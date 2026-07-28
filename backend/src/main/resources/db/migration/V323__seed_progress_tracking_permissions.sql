-- Permissions for portion-completion progress logging/reporting (Phase 3 of the Timetable
-- planner Round 2 initiative). Kept as two distinct operations per the operation-wise permission
-- mapping gate: logging your own session's coverage is a different action, for a different
-- audience (any faculty teaching), than viewing a cross-faculty subject-wise progress report
-- (an admin/coordinator concern) -- never conflated under one shared permission.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('PROGRESS_LOG_CREATE',  'Log Syllabus Progress',        'CURRICULUM', 'My Timetable',      CURRENT_TIMESTAMP),
    ('PROGRESS_REPORT_VIEW', 'View Subject Progress Report', 'CURRICULUM', 'Progress Report',   CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Log Progress travels with the ability to see one's own timetable.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'PROGRESS_LOG_CREATE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- Progress Report viewing travels with curriculum/timetable management oversight.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'CURRICULUM_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'PROGRESS_REPORT_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'PROGRESS_REPORT_VIEW') new_p
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
