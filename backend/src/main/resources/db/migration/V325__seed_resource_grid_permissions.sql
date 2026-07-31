-- Master resource-matrix views (Timetable planner Round 2, Phase 5): rows = every active
-- faculty/room at once, not a single-resource filtered grid. Two dedicated permissions per the
-- operation-wise mapping gate -- someone might legitimately see room utilization without seeing
-- every faculty's personal schedule side-by-side, or vice versa.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_FACULTY_GRID_VIEW',   'View Faculty Resource Grid',   'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_CLASSROOM_GRID_VIEW', 'View Classroom Resource Grid', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code IN
    ('TIMETABLE_FACULTY_GRID_VIEW', 'TIMETABLE_CLASSROOM_GRID_VIEW')) new_p
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
