-- OC-256: Skeleton Builder's "Total Unassigned" hours stat was purely a display figure -- nothing
-- downstream (unstaffedCount / unassignedOfferingCount / the Conflict Inspector scan) ever
-- compared placed hours against curriculum-required hours, so a cohort with, say, its whole
-- Theory/Lab budget unscheduled could be published without warning as long as whatever sessions
-- DID exist were staffed and conflict-free. TimetableGenerationService#approve now checks this via
-- TimetableCoverageService and refuses by default (TimetableCoverageGapException) -- this
-- dedicated permission is who may resubmit with overrideIncompleteCoverage=true and a reason
-- instead, per the operation-wise permission mapping hard gate (this bypasses a safety check, so
-- it is its own permission, never folded into TIMETABLE_MANAGE itself).
--
-- Tier 3 (Hold Only), matching DOCUMENT_VERIFIED_OVERRIDE (V230) -- the closest existing precedent
-- for "override an intentional completeness/compliance gate": any senior role can hold and use it,
-- but only Support+ can delegate it to a sub-role.

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE', 'Approve Timetable With Incomplete Coverage', 'CURRICULUM', 'Timetable', 3, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN only -- same top-level-admin restriction as
-- DOCUMENT_VERIFIED_OVERRIDE, since this also bypasses an intentional completeness check.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
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
