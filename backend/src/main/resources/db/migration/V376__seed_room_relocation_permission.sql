-- Permission for the single-date Room Relocation Modal -- swaps the room for one specific
-- calendar occurrence without touching the recurring ClassSchedule's own venue.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_ROOM_RELOCATE', 'Relocate Room for a Session Date', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Travels with general timetable management oversight, same anchor as TIMETABLE_STAFF_SWAP (V330).
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_ROOM_RELOCATE') new_p
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
