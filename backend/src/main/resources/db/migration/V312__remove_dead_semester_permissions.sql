-- SEMESTER_VIEW/CREATE/EDIT/DELETE are dead: no @PreAuthorize check anywhere in the backend
-- and no frontend route/service references them. Leftover from before V118 renamed the
-- Semester concept to Term (TermInstance/TermInstanceController). SEMESTER_MANAGE is kept —
-- TermInstanceController.update() still enforces it and academic-year-form.component.ts
-- still calls it for term date edits / status advancement.
-- role_permissions.permission_id has ON DELETE CASCADE (V87), so no separate cleanup needed there.
DELETE FROM permissions WHERE code IN ('SEMESTER_VIEW', 'SEMESTER_CREATE', 'SEMESTER_EDIT', 'SEMESTER_DELETE');
