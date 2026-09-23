-- My Timetable is being split into two separate screens/nav entries: "My Timetable (Student)"
-- and "My Timetable (Staff)" -- each gets its own view type (Generic/Date-wise-weekly/Day) and
-- staff's retains the Log Progress action that students' doesn't. Per the operation-wise
-- permission mapping rule, two distinct screens get two distinct permissions rather than sharing
-- the existing MY_TIMETABLE_VIEW. MY_TIMETABLE_VIEW itself is left in place (still referenced by
-- TimetableController#me/#occurrences) rather than removed, since permissions are never deleted
-- once shipped. Column names verified against the most recent permission-seeding migration
-- (V540__seed_timetable_publish_discard_permissions.sql): permissions(code, display_name,
-- category, screen_label, created_at), role_permissions(role_id, permission_id).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('MY_TIMETABLE_VIEW_STUDENT', 'View My Timetable (Student)', 'CURRICULUM', 'My Timetable (Student)', CURRENT_TIMESTAMP),
    ('MY_TIMETABLE_VIEW_STAFF', 'View My Timetable (Staff)', 'CURRICULUM', 'My Timetable (Staff)', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Backfill: this is a screen split, not a new restriction -- every role that already holds
-- MY_TIMETABLE_VIEW keeps equivalent access on both new split screens, same as V540's
-- TIMETABLE_MANAGE -> TIMETABLE_PUBLISH/DISCARD_DRAFT backfill. Nobody should silently lose
-- capability at cutover. This deliberately does NOT auto-grant STUDENT/FACULTY beyond what they
-- already hold, per V515's recorded decision that viewing even your own timetable is not
-- self-service by default.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'MY_TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code IN ('MY_TIMETABLE_VIEW_STUDENT', 'MY_TIMETABLE_VIEW_STAFF')) new_p
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
