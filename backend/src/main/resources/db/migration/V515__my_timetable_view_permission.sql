-- ============================================================
-- V515: My Timetable self-scoped view permission (mechanism only, NOT
-- auto-granted to STUDENT/FACULTY)
-- ============================================================
-- Companion to V514's MY_ATTENDANCE_VIEW/MY_EXAM_RESULT_VIEW self-service
-- permissions for the Student Portal (item 15). This one is deliberately
-- NOT granted to STUDENT here, unlike those two: V294__seed_timetable_permissions
-- already recorded an explicit prior specialist-review decision that "viewing
-- even your own timetable is not self-service" and TIMETABLE_VIEW must be
-- granted manually via Role Management, not auto-granted. Auto-granting
-- MY_TIMETABLE_VIEW to STUDENT here would silently overturn that recorded
-- decision without the human review it was given the first time.
--
-- This migration only creates the permission and (per the mandatory
-- DEV_ADMIN/SUPPORT_ADMIN catch-all pattern) grants it to the two platform
-- system roles. TimetableController's /me and /occurrences?scope=personal
-- now accept MY_TIMETABLE_VIEW as an alternative to TIMETABLE_VIEW (with
-- /occurrences still requiring TIMETABLE_VIEW for scope=browse, so this
-- narrower permission can never see the whole-term listing). Whoever decides
-- students should see their own timetable grants MY_TIMETABLE_VIEW to
-- STUDENT explicitly via the DB-only Role Management module.
-- ============================================================

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('MY_TIMETABLE_VIEW', 'View My Timetable', 'CURRICULUM', 'My Timetable', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
