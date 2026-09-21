-- Permission for the new on-demand "Run Now" trigger on the reorder-breach auto-indent job
-- (AutoIndentService) — Phase C of the Stock Indent auto-indent feature. Its own operation-wise
-- permission, separate from _MANAGE, matching Wanted List's own INVENTORY_WANTED_LIST_RUN
-- (V437) — running the detection job is a distinct, audit-worthy action from editing an
-- individual indent.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_STOCK_INDENT_AUTO_RUN', 'Run Stock Indent Auto-Detection', 'MASTER', 'Stock Indents', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_STOCK_INDENT_AUTO_RUN'
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
