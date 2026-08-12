-- Permissions for the Special/Remedial Class Scheduler (BR-55). Kept as four distinct operations
-- per the operation-wise permission mapping gate: requesting, viewing (own requests / the
-- approval queue), approving/rejecting, and cancelling are each a different action for a
-- different audience -- never conflated under one shared permission.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SPECIAL_CLASS_REQUEST', 'Request Special/Remedial Class',  'CURRICULUM', 'My Special Classes',       CURRENT_TIMESTAMP),
    ('TIMETABLE_SPECIAL_CLASS_VIEW',    'View Special Class Requests',     'CURRICULUM', 'My Special Classes',       CURRENT_TIMESTAMP),
    ('TIMETABLE_SPECIAL_CLASS_CANCEL',  'Cancel Special Class Request',    'CURRICULUM', 'My Special Classes',       CURRENT_TIMESTAMP),
    ('TIMETABLE_SPECIAL_CLASS_APPROVE', 'Approve/Reject Special Classes',  'CURRICULUM', 'Special Class Approvals',  CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Requesting/viewing-own/cancelling a special class travels with the ability to see one's own
-- timetable -- same anchor PROGRESS_LOG_CREATE already uses (V323).
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code IN
    ('TIMETABLE_SPECIAL_CLASS_REQUEST', 'TIMETABLE_SPECIAL_CLASS_VIEW', 'TIMETABLE_SPECIAL_CLASS_CANCEL')) new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- Approving/rejecting travels with general timetable management oversight.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SPECIAL_CLASS_APPROVE') new_p
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
