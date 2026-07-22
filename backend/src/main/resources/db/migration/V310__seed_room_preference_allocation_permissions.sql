-- Permissions for Room Preference (R2-4.1.3) and Room Allocation (R2-4.1.4). Kept as their own
-- rows per the operation-wise permission mapping gate -- distinct from CAMPUS_INFRASTRUCTURE_*
-- and HOSTEL_ROOM_* since submitting/managing a preference and creating a binding allocation are
-- separate operations, likely touched by a different staff population (front-office/admission
-- staff for preferences, warden/hostel-admin for allocations) than general infra maintenance.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('HOSTEL_ROOM_PREFERENCE_VIEW',   'View Room Preferences',   'MASTER', 'Room Preferences', CURRENT_TIMESTAMP),
    ('HOSTEL_ROOM_PREFERENCE_MANAGE', 'Manage Room Preferences', 'MASTER', 'Room Preferences', CURRENT_TIMESTAMP),
    ('HOSTEL_ROOM_ALLOCATION_VIEW',   'View Room Allocations',   'MASTER', 'Room Allocations', CURRENT_TIMESTAMP),
    ('HOSTEL_ROOM_ALLOCATION_MANAGE', 'Manage Room Allocations', 'MASTER', 'Room Allocations', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('HOSTEL_ROOM_PREFERENCE_VIEW', 'HOSTEL_ROOM_PREFERENCE_MANAGE',
                 'HOSTEL_ROOM_ALLOCATION_VIEW', 'HOSTEL_ROOM_ALLOCATION_MANAGE')
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
