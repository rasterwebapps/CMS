-- Permission for the new Conflict Inspector dashboard (OC-125): a whole-term scan of every
-- structural violation (room/faculty conflicts, workload caps, faculty availability, blocked
-- periods, capacity fit) already reachable in staffing, now also re-run as a hard gate on
-- TimetableGenerationService.approve(). Read-only screen, single VIEW permission.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_CONFLICT_INSPECTOR_VIEW', 'View Timetable Conflict Inspector', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Default tier: same anchor Capacity Planner's own VIEW permission used (V346) -- anyone who can
-- staff a timetable should be able to see what's blocking its publish.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_STAFFING_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CONFLICT_INSPECTOR_VIEW') new_p
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
