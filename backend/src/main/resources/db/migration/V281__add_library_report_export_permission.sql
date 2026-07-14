-- V281: Overdue Books report gains pagination + export (replacing the old client-side-only
-- Fine Summary / Issue History / Accession Register tabs, which duplicated the existing
-- Fines, Issue Explorer, and Book Explorer screens). Per the operation-wise permission
-- mapping hard gate, Export is its own dedicated permission, never folded into
-- LIBRARY_REPORT_VIEW.

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('LIBRARY_REPORT_EXPORT', 'Export Library Reports', 'LIBRARY', 'Library Reports', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign to roles that already hold LIBRARY_REPORT_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_REPORT_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_REPORT_EXPORT') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
