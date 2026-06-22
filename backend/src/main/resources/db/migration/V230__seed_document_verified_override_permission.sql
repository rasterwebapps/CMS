-- V230: Seed DOCUMENT_VERIFIED_OVERRIDE permission, allowing an authorized
-- reviewer to force-replace a VERIFIED admission/faculty document (resetting
-- its verification status) without first manually unverifying it. BR-26
-- otherwise treats VERIFIED as a terminal, read-only state.

INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('DOCUMENT_VERIFIED_OVERRIDE', 'Force-Replace Verified Documents', 'DOCUMENT', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN only: this bypasses an
-- intentional compliance lock, so it is restricted to top-level admin roles.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'DOCUMENT_VERIFIED_OVERRIDE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
