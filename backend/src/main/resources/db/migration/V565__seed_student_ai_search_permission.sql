-- New dedicated permission for AI Smart Search (natural-language student search via a
-- locally-hosted Ollama model), per the operation-wise permission mapping hard gate -- not
-- reused from STUDENT_VIEW (V88). Defaults to the same functional tier as STUDENT_VIEW (closest
-- match), auto-granted to whoever already holds it.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('STUDENT_AI_SEARCH_VIEW', 'AI Smart Search (Students)', 'ADMISSION', 'AI Smart Search', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'STUDENT_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'STUDENT_AI_SEARCH_VIEW') new_p
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
