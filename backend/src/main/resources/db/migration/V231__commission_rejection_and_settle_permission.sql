-- V231: Support rejecting a commission payout (with reason) and reopening it,
-- plus a COMMISSION_SETTLE permission for recording the actual cash/other-mode
-- payout separately from the admin's approve/reject power.

ALTER TABLE enquiries
    ADD COLUMN IF NOT EXISTS commission_rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS commission_rejected_by VARCHAR(150),
    ADD COLUMN IF NOT EXISTS commission_rejected_at TIMESTAMP;

INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('COMMISSION_SETTLE', 'Record Commission Payout Settlement', 'FINANCE', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN catch-all sync only.
-- Assignment to other roles (e.g. CASHIER) is left to the Role Management module.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'COMMISSION_SETTLE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
