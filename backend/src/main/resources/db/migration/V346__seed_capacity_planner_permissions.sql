-- Permissions for the new Capacity Planner screen: viewing the room/lab/clinical sizing report
-- (TIMETABLE_CAPACITY_PLANNER_VIEW) and the "Create Suggested Batches" bulk action
-- (TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE) — each button/operation gets its own dedicated
-- permission, never shared. VIEW defaults to the same tier as TIMETABLE_STAFFING_MANAGE (any role
-- that can staff a timetable should be able to see the sizing report); BATCH_CREATE defaults to
-- the same tier as Batch management (creating batches is already a Batch-manage-tier action, this
-- is just a bulk-create trigger for the same underlying capability).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_CAPACITY_PLANNER_VIEW', 'View Timetable Capacity Planner', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE', 'Create Suggested Batches', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_STAFFING_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CAPACITY_PLANNER_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_STAFFING_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE') new_p
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
