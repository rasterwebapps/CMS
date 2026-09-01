-- OC-175/OC-178: faculty escort-duty rotation reuses rotation_groups + rotation_members as-is
-- (both already fully generic -- termInstance/label/cycleLength/anchorDate and
-- rotationGroup/memberOrder/label carry nothing student- or Batch-specific). One RotationGroup per
-- clinical Batch's escort pool (cycle_length = eligible faculty count), one RotationMember per
-- eligible faculty. Only rotation_slots/rotation_member_assignments (hard-wired to
-- class_schedules/batches for the N-slots x N-members interleaved-subject shape) don't fit --
-- escort duty is a single recurring duty x N faculty, a degenerate 1-slot case of the same parity
-- math (see com.cms.util.RotationParity), so a minimal new table replaces that pair instead.
CREATE TABLE escort_rotation_assignments (
    id                   BIGSERIAL PRIMARY KEY,
    rotation_member_id   BIGINT NOT NULL REFERENCES rotation_members(id) ON DELETE CASCADE,
    batch_id             BIGINT NOT NULL REFERENCES batches(id),
    faculty_id           BIGINT NOT NULL REFERENCES faculty(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_escort_rotation_member UNIQUE (rotation_member_id),
    CONSTRAINT ux_escort_rotation_batch_faculty UNIQUE (batch_id, faculty_id)
);

CREATE INDEX idx_escort_rotation_assignments_batch ON escort_rotation_assignments(batch_id);
CREATE INDEX idx_escort_rotation_assignments_faculty ON escort_rotation_assignments(faculty_id);

-- Permissions: dedicated VIEW/MANAGE, same closest-match tiers as V410's Clinical Shift Group
-- permissions and V363's Batch Rotation precedent.
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_ESCORT_ROTATION_VIEW', 'View Escort Rotation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_ESCORT_ROTATION_MANAGE', 'Manage Escort Rotation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_ESCORT_ROTATION_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_ESCORT_ROTATION_MANAGE') new_p
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
