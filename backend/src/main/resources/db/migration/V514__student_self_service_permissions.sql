-- ============================================================
-- V514: Student self-service ("My Attendance" / "My Exam Results") permissions
-- ============================================================
-- Groundwork for the Student Portal (item 15): new self-scoped read-only
-- permissions, granted directly to the existing STUDENT role (id 20), which
-- already holds MY_LIBRARY_VIEW for the same self-service pattern.
-- ============================================================

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('MY_ATTENDANCE_VIEW',  'View My Attendance',   'CURRICULUM',  'My Attendance',    4, CURRENT_TIMESTAMP),
    ('MY_EXAM_RESULT_VIEW', 'View My Exam Results', 'EXAMINATION', 'My Exam Results',  4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant directly to STUDENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'STUDENT'
  AND p.code IN ('MY_ATTENDANCE_VIEW', 'MY_EXAM_RESULT_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
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
