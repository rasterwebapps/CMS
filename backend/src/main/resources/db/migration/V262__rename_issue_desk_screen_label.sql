-- V262: Nav label "Issue Desk" renamed to "Issue Explorer" (cosmetic). Updates the
-- permissions.screen_label value set by V247 (and reused by V257's seed) so the Role
-- Management screen (which groups permissions by screen_label) stays consistent with
-- the new nav label — those earlier migrations are already shipped and must not be
-- edited in place.
UPDATE permissions
SET screen_label = 'Issue Explorer'
WHERE screen_label = 'Issue Desk';
