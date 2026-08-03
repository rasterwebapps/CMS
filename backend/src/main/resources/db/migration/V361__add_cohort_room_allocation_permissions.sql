-- Permissions for the new Cohort Room Allocation feature (Capacity Planner's new
-- "commit a cohort's physical Theory/Lab/Clinical rooms" step). VIEW/MANAGE/REVERT are
-- each their own dedicated permission -- never shared -- per the operation-wise
-- permission mapping convention. VIEW and MANAGE default to the same tier as
-- TIMETABLE_CAPACITY_PLANNER_VIEW/TIMETABLE_STAFFING_MANAGE respectively; REVERT is a
-- distinct, higher-risk operation (undoes a committed allocation and deactivates real
-- batches) and also defaults to the TIMETABLE_STAFFING_MANAGE tier, not auto-granted
-- alongside plain MANAGE.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_COHORT_ROOM_ALLOCATION_VIEW', 'View Cohort Room Allocation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE', 'Commit Cohort Room Allocation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT', 'Revert Cohort Room Allocation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_CAPACITY_PLANNER_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_COHORT_ROOM_ALLOCATION_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_STAFFING_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_STAFFING_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT') new_p
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
