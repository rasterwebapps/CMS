-- Permission for the generalized single-date staff-to-staff session swap (Timetable planner
-- Round 2, Phase 7). Deliberately NOT the same code as TIMETABLE_SWAP -- that permission (V314)
-- is documented and scoped as DRAFT-only day/period swapping; this is a distinct operation
-- (published-schedule, single-date, faculty-to-faculty) per the operation-wise mapping gate.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_STAFF_SWAP', 'Swap Staff Between Sessions', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_STAFF_SWAP') new_p
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
