-- New dedicated permission for viewing the full Special Class Approvals history (all statuses,
-- filterable by faculty/date-range/subject/cohort/search), per the operation-wise permission
-- mapping hard gate -- not reused from TIMETABLE_SPECIAL_CLASS_APPROVE (V375). Viewing the list is
-- kept a separate operation from approving/rejecting, same split already used for
-- FACULTY_ABSENCE_VIEW vs FACULTY_ABSENCE_MARK (V563). Defaults to the same functional tier as
-- TIMETABLE_SPECIAL_CLASS_APPROVE (closest match), auto-granted to whoever already holds it.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_SPECIAL_CLASS_HISTORY_VIEW', 'View Special Class Request History', 'CURRICULUM', 'Special Class Approvals', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SPECIAL_CLASS_APPROVE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_SPECIAL_CLASS_HISTORY_VIEW') new_p
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
