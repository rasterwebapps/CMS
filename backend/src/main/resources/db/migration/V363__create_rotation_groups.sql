-- Batch rotation: lets N physical groups of students alternate, week to week, through N
-- parallel same-day-same-period ClassSchedule rows across different subjects/specialities (e.g.
-- Batch 1 attends English Lab this week / Tamil Lab next week, Batch 2 reversed; or 3+ clinical
-- speciality batches rotating). Faculty and venue for a given ClassSchedule row never change --
-- only which existing per-subject Batch (and its roster) occupies that row on a given date. See
-- RotationResolverService for the parity math. The day-swap rotation shape (e.g. Batch 1 ->
-- English Wed / Tamil Thu) needs none of this -- it's just independently-placed ClassSchedule
-- rows, each with its own fixed batch, already supported today.
CREATE TABLE rotation_groups (
    id                      BIGSERIAL PRIMARY KEY,
    term_instance_id        BIGINT NOT NULL REFERENCES term_instances(id),
    label                   VARCHAR(150) NOT NULL,
    cycle_length            INT NOT NULL,
    anchor_occurrence_date  DATE NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_by              VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rotation_group_cycle_length CHECK (cycle_length >= 2)
);

CREATE INDEX idx_rotation_groups_term ON rotation_groups(term_instance_id);

-- One row per ClassSchedule cell that participates in the rotation (e.g. "English Lab, Wed
-- P3-4" and "Tamil Lab, Wed P3-4"). A cell belongs to at most one rotation group.
CREATE TABLE rotation_slots (
    id                  BIGSERIAL PRIMARY KEY,
    rotation_group_id   BIGINT NOT NULL REFERENCES rotation_groups(id) ON DELETE CASCADE,
    class_schedule_id   BIGINT NOT NULL UNIQUE REFERENCES class_schedules(id),
    slot_order          INT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_rotation_slot_order UNIQUE (rotation_group_id, slot_order)
);

CREATE INDEX idx_rotation_slots_group ON rotation_slots(rotation_group_id);

-- One row per physical group of students rotating through the slots above (e.g. "Batch 1",
-- "Batch 2"). member_order fixes each group's starting offset in the rotation.
CREATE TABLE rotation_members (
    id                  BIGSERIAL PRIMARY KEY,
    rotation_group_id   BIGINT NOT NULL REFERENCES rotation_groups(id) ON DELETE CASCADE,
    member_order        INT NOT NULL,
    label               VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_rotation_member_order UNIQUE (rotation_group_id, member_order)
);

CREATE INDEX idx_rotation_members_group ON rotation_members(rotation_group_id);

-- Which existing per-subject Batch represents a given member when its turn lands on a given
-- slot -- reuses that Batch's own roster/capacity/venue entirely, no new roster table.
CREATE TABLE rotation_member_assignments (
    id                   BIGSERIAL PRIMARY KEY,
    rotation_member_id   BIGINT NOT NULL REFERENCES rotation_members(id) ON DELETE CASCADE,
    rotation_slot_id     BIGINT NOT NULL REFERENCES rotation_slots(id) ON DELETE CASCADE,
    batch_id             BIGINT NOT NULL REFERENCES batches(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_rotation_assignment_member_slot UNIQUE (rotation_member_id, rotation_slot_id),
    CONSTRAINT ux_rotation_assignment_slot_batch UNIQUE (rotation_slot_id, batch_id)
);

CREATE INDEX idx_rotation_member_assignments_member ON rotation_member_assignments(rotation_member_id);
CREATE INDEX idx_rotation_member_assignments_slot ON rotation_member_assignments(rotation_slot_id);
CREATE INDEX idx_rotation_member_assignments_batch ON rotation_member_assignments(batch_id);

-- Permissions: VIEW and MANAGE are each their own dedicated permission, never shared, per the
-- operation-wise permission mapping convention. VIEW defaults to the TIMETABLE_VIEW tier (same
-- as Skeleton Builder/Staffing); MANAGE defaults to the TIMETABLE_SKELETON_MANAGE tier, since
-- rotation setup is an extension of skeleton building.
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_ROTATION_VIEW', 'View Batch Rotation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_ROTATION_MANAGE', 'Manage Batch Rotation', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_ROTATION_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_ROTATION_MANAGE') new_p
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
