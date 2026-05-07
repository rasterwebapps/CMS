-- V103: Add DB-driven permissions for Scholarship Management.

INSERT INTO permissions (code, display_name, category, description)
SELECT 'SCHOLARSHIP_VIEW', 'View Scholarships', 'SCHOLARSHIP', 'View scholarship types, eligibility and student scholarship records'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'SCHOLARSHIP_VIEW');

INSERT INTO permissions (code, display_name, category, description)
SELECT 'SCHOLARSHIP_MANAGE', 'Manage Scholarship Types', 'SCHOLARSHIP', 'Create, update and deactivate scholarship master records and eligibility details'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'SCHOLARSHIP_MANAGE');

INSERT INTO permissions (code, display_name, category, description)
SELECT 'SCHOLARSHIP_APPLY', 'Apply for Scholarship', 'SCHOLARSHIP', 'Submit and renew scholarship applications'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'SCHOLARSHIP_APPLY');

INSERT INTO permissions (code, display_name, category, description)
SELECT 'SCHOLARSHIP_APPROVE', 'Approve/Reject Scholarship Applications', 'SCHOLARSHIP', 'Approve, reject and verify scholarship eligibility/applications'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'SCHOLARSHIP_APPROVE');

INSERT INTO permissions (code, display_name, category, description)
SELECT 'SCHOLARSHIP_DISBURSE', 'Record Scholarship Disbursements', 'SCHOLARSHIP', 'Record scholarship disbursements and fee waivers'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'SCHOLARSHIP_DISBURSE');

-- Full access for system/application administrators and college admins.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code IN (
    'SCHOLARSHIP_VIEW',
    'SCHOLARSHIP_MANAGE',
    'SCHOLARSHIP_APPLY',
    'SCHOLARSHIP_APPROVE',
    'SCHOLARSHIP_DISBURSE'
)
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Front office can view and apply during admission workflows.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code IN ('SCHOLARSHIP_VIEW', 'SCHOLARSHIP_APPLY')
WHERE r.name = 'FRONT_OFFICE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Cashier can view and record disbursements/fee waivers.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code IN ('SCHOLARSHIP_VIEW', 'SCHOLARSHIP_DISBURSE')
WHERE r.name = 'CASHIER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Faculty and students get read-only visibility.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code = 'SCHOLARSHIP_VIEW'
WHERE r.name IN ('FACULTY', 'STUDENT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

