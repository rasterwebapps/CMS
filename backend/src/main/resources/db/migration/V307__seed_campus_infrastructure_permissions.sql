-- Permissions for the new Campus Infrastructure hierarchy (Block/Floor/Zone/Room) and the
-- distinct Hostel Room attachment action. Kept separate per the operation-wise permission
-- mapping gate: assigning a room type to a room is a hostel-domain action (likely a different
-- staff population than whoever maintains the general campus building/room inventory), not a
-- reuse of CAMPUS_INFRASTRUCTURE_MANAGE. Both granted explicitly to admin-tier roles, matching
-- the Designation/Hostel Room Type master pattern — first screens of their kind, nothing to
-- auto-inherit from.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('CAMPUS_INFRASTRUCTURE_VIEW',   'View Campus Infrastructure',   'MASTER', 'Campus Infrastructure', CURRENT_TIMESTAMP),
    ('CAMPUS_INFRASTRUCTURE_MANAGE', 'Manage Campus Infrastructure', 'MASTER', 'Campus Infrastructure', CURRENT_TIMESTAMP),
    ('HOSTEL_ROOM_VIEW',             'View Hostel Room Assignment',   'MASTER', 'Hostel Rooms', CURRENT_TIMESTAMP),
    ('HOSTEL_ROOM_MANAGE',           'Manage Hostel Room Assignment', 'MASTER', 'Hostel Rooms', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('CAMPUS_INFRASTRUCTURE_VIEW', 'CAMPUS_INFRASTRUCTURE_MANAGE', 'HOSTEL_ROOM_VIEW', 'HOSTEL_ROOM_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
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
