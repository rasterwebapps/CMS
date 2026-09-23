-- ============================================================
-- V521: Student self-service ("My Fees") permission
-- ============================================================
-- Third self-service slice for the Student Portal (item 15), same shape as V514's
-- MY_ATTENDANCE_VIEW/MY_EXAM_RESULT_VIEW: granted directly to the existing STUDENT
-- role (id 20). Scope was an explicit specialist-review decision -- full fee ledger
-- + payment history + outstanding penalties, not just current dues -- backed by
-- /student-fees/my/summary, /my/receipts, /my/penalties (StudentFeeController),
-- all resolving the caller's own linked Student via the app_users FK, never a
-- client-supplied studentId. Unlike MY_TIMETABLE_VIEW (V515), there is no prior
-- specialist-review precedent withholding this, so it's auto-granted here rather
-- than left for a separate Role Management decision.
-- ============================================================

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('MY_FEE_VIEW', 'View My Fees', 'FINANCE', 'My Fees', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant directly to STUDENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'STUDENT'
  AND p.code = 'MY_FEE_VIEW'
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
