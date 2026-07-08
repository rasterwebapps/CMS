-- V258: Revoke LIBRARY_CATALOGUE_EXPORT / LIBRARY_ISSUE_EXPORT from STUDENT.
--
-- These permission codes were seeded back in V242 (granular_screen_permissions) as part of a
-- blanket "grant _EXPORT to any role holding the matching _VIEW/_CREATE/_EDIT/_DELETE code"
-- backfill. At that time no code actually checked these permissions, so the over-grant was
-- dormant. V257 wired real /export endpoints behind these exact codes, which turned this into
-- a live access-control bug: STUDENT holds LIBRARY_CATALOGUE_VIEW and LIBRARY_ISSUE_VIEW (V197),
-- so V242's old backfill silently gave students bulk data-export access to the book catalogue
-- and issue register — export was explicitly decided to be manage-tier-only, not granted to
-- FACULTY/STUDENT view-only access.
--
-- LIBRARY_FINE_EXPORT / LIBRARY_PERIODICAL_EXPORT are unaffected — STUDENT never held
-- LIBRARY_FINE_VIEW or LIBRARY_PERIODICAL_VIEW, so V242's backfill never touched those for STUDENT.

DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM app_roles WHERE name = 'STUDENT')
  AND permission_id IN (
      SELECT id FROM permissions WHERE code IN ('LIBRARY_CATALOGUE_EXPORT', 'LIBRARY_ISSUE_EXPORT')
  );
