-- Conflict Inspector acknowledgment gate for Draft Review's Approve/Publish action.
--
-- Skeleton Builder -> Conflict Inspector -> Draft Review is meant to be a sequential flow: a term
-- cannot be approved until an admin has actually opened Conflict Inspector after the current
-- skeleton and seen a clean (zero-violation) scan. TimetableGenerationService#approve already
-- re-runs the same scan and blocks on any violation, but that alone doesn't guarantee a human
-- looked at the dashboard again after the *last* edit if that edit happened to introduce no new
-- violations -- these two columns record when that look last happened and how many active cells
-- existed at the time, so a later placement/removal (which changes the count) or edit (which bumps
-- class_schedules.updated_at) invalidates the acknowledgment even without introducing a violation.
ALTER TABLE term_instances
    ADD COLUMN IF NOT EXISTS conflict_acknowledged_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS conflict_acknowledged_cell_count INTEGER NULL;

-- "Proceed to Review" in Conflict Inspector is its own operation distinct from viewing the scan
-- (TIMETABLE_CONFLICT_INSPECTOR_VIEW already exists), per the operation-wise permission mapping
-- rule. Defaulted to whichever roles already hold TIMETABLE_MANAGE, since acknowledging is only
-- ever useful to someone who can also reach Approve.
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE', 'Acknowledge Timetable Conflict Inspector', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE') new_p
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
