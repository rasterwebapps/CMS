-- Lets ADMIN rename any user's display name (app_users.full_name) via a dedicated
-- PUT /user-management/{id}/name endpoint that deliberately bypasses the normal hierarchy gate —
-- needed because ADMIN cannot otherwise even see or edit accounts at/above its own hierarchy
-- level (e.g. the seeded DEV_ADMIN/SUPPORT_ADMIN accounts), yet still needs a way to correct a
-- wrong display name on any account. Every other edit operation (role, email, active status)
-- remains exactly as hierarchy-restricted as before — this permission only unlocks renaming.

-- Tier 3 ("Hold Only — any senior role holds it; only Support+ can delegate it"), matching other
-- permissions that bypass a normal safety control (STUDENT_DELETE, FEE_FINALIZE in V241) rather
-- than USER_EDIT's own open-by-default tier 4 — this one is meaningfully more sensitive than
-- Edit since it deliberately bypasses the hierarchy gate.
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('USER_RENAME', 'Rename Any User', 'SYSTEM', 'User Management', 3, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code = 'USER_RENAME'
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
