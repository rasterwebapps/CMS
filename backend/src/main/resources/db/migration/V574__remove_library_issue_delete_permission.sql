-- V574: Remove LIBRARY_ISSUE_DELETE permission.
-- Circulation records in library_issues are never hard-deleted; mistaken issues
-- are closed via the normal Return workflow with remarks.
-- role_permissions.permission_id has ON DELETE CASCADE (V87), so role assignments
-- are cleaned up automatically.
DELETE FROM permissions WHERE code = 'LIBRARY_ISSUE_DELETE';
