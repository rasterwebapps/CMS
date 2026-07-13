-- V261: Permission for the new "View History" action on the Book Catalogue and
-- Journals & Periodicals list screens (full acquisition + shelf-transfer + circulation
-- timeline for a single item). This is its own operation, not folded into CATALOGUE_MANAGE
-- (operation-wise permission mapping hard gate) — and since it surfaces the same
-- borrower/circulation PII as the existing student/faculty issue-history endpoints
-- (which are LIBRARY_ISSUE_MANAGE-gated), it's tier-matched to that, not to
-- LIBRARY_CATALOGUE_MANAGE.

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('LIBRARY_CATALOGUE_VIEW_HISTORY',  'View Book History',    'LIBRARY', 'Book Catalogue', 4, CURRENT_TIMESTAMP),
    ('LIBRARY_PERIODICAL_VIEW_HISTORY', 'View Journal History', 'LIBRARY', 'Journals',        4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign to roles that already hold LIBRARY_ISSUE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_ISSUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code IN ('LIBRARY_CATALOGUE_VIEW_HISTORY', 'LIBRARY_PERIODICAL_VIEW_HISTORY')) new_p
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
