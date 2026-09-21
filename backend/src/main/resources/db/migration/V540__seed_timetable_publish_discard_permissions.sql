-- OC-260: Timetable Draft Review is being retired into Skeleton Builder. Approve/Publish and
-- Discard-Draft (TimetableController#approve, #clear) were gated by the generic TIMETABLE_MANAGE
-- (the same permission that gates build/edit actions), which conflates three operations behind
-- one permission -- against the operation-wise permission mapping rule. Split into their own
-- dedicated permissions here; TIMETABLE_DISCARD_PUBLISHED already exists as its own permission for
-- Revert-to-Draft (see the migration that added TimetableController#revertToDraft) and is left as
-- is. Column names verified against the most recent permission-seeding migration
-- (V538__seed_inventory_stock_indent_fulfill_permission.sql): permissions(code, display_name,
-- category, screen_label, created_at), role_permissions(role_id, permission_id).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_PUBLISH', 'Publish Timetable', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_DISCARD_DRAFT', 'Discard Draft Timetable', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Backfill: every role that already holds TIMETABLE_MANAGE keeps the ability to publish/discard it
-- previously had implicitly through that permission -- this is a permission split, not a new
-- restriction, so no one should silently lose capability at cutover. Going forward, granting these
-- to any other role is DB-only via Role Management, same as every other permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code IN ('TIMETABLE_PUBLISH', 'TIMETABLE_DISCARD_DRAFT')) new_p
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
