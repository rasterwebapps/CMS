-- Fee Explorer's "Export to Excel" only ever showed a per-student aggregate Pending figure.
-- Client request: a semester-wise breakdown (Fee/Paid/Pending per semester, one row per student
-- per semester) so pending balances can be found at the semester level. Added as a NEW export
-- option alongside the existing whole/aggregate export, not a replacement.
--
-- Own dedicated permission per the operation-wise permission mapping hard gate — this is a
-- distinct export operation on the Fee Explorer screen, never folded into STUDENT_FEE_EXPORT
-- itself. Tier defaults to the same tier as STUDENT_FEE_EXPORT (closest existing match), and is
-- auto-granted to any role that already holds STUDENT_FEE_EXPORT so no existing user loses
-- today's effective export access.

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('STUDENT_FEE_EXPORT_SEMESTER_WISE', 'Export Fee Records (Semester-wise)', 'FINANCE', 'Fee Explorer', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'STUDENT_FEE_EXPORT'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'STUDENT_FEE_EXPORT_SEMESTER_WISE') new_p
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
