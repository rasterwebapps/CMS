-- Permission for the new "Auto-Assign" bulk Theory faculty action (Assign Faculty screen +
-- automatic run at Capacity Auto-Plan commit). Distinct from SECTION_FACULTY_MANAGE per the
-- operation-wise permission mapping rule -- auto-assign touches every unassigned offering in a
-- term at once, a materially bigger blast radius than one manual per-row save, so it gets its own
-- dedicated permission rather than reusing MANAGE. Defaults to whichever role already holds
-- SECTION_FACULTY_MANAGE (the closest existing tier), mirroring V392's own COURSE_MANAGE ->
-- SECTION_FACULTY_MANAGE grant pattern.
--
-- Authored as V478, renumbered to V499, then to V502 -- neither earlier number was ever deployed.
-- 478 was abandoned because V479-V497 shipped while this file sat uncommitted in a working tree, so
-- landing it there would have been an out-of-order insertion: `out-of-order` is unset (defaults
-- false) and 243/prod run with validate-on-migrate on, which would crash-loop the backend. 499 then
-- collided head-on with V499__add_variant_to_stock_transfer_lines.sql, authored concurrently in the
-- same working tree, and Flyway refuses to boot at all on a duplicate version ("Found more than one
-- migration with version 499") -- so this pair moved above the inventory run (V497-V500) rather than
-- renumbering someone else's in-flight work. Local dev has a 478 history row from the original file;
-- this is idempotent, so re-running it at 502 is a no-op.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('SECTION_FACULTY_AUTO_ASSIGN', 'Auto-Assign Section Faculty', 'MASTER', 'Course Offerings', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'SECTION_FACULTY_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'SECTION_FACULTY_AUTO_ASSIGN') new_p
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
