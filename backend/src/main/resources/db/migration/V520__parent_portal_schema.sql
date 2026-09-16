-- ============================================================
-- V516: Parent Portal (item 16) -- first slice: schema + PARENT role
-- ============================================================
-- Per docs/PARENT_PORTAL_DESIGN_PROPOSAL.md, approved by human specialist
-- review 2026-09-16: a dedicated Guardian entity (not the existing free-text
-- Student.fatherEmail/motherEmail columns, which are unvalidated, not
-- unique, and assume exactly two guardians in fixed roles) linked to
-- students via a many-to-many join table, since the real cardinality is
-- one guardian -> many wards (siblings) and one student -> many guardians.
-- ============================================================

CREATE TABLE guardians (
    id                BIGSERIAL PRIMARY KEY,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    phone             VARCHAR(20),
    relationship_hint VARCHAR(50),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE student_guardians (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    guardian_id  BIGINT NOT NULL REFERENCES guardians(id) ON DELETE CASCADE,
    is_primary   BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_guardians_pair UNIQUE (student_id, guardian_id)
);

-- Same shape as app_users.student_id/faculty_id: nullable FK, unique only
-- when set (one login account per guardian, one guardian per login account).
ALTER TABLE app_users ADD COLUMN guardian_id BIGINT REFERENCES guardians(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX uq_app_users_guardian_id ON app_users (guardian_id) WHERE guardian_id IS NOT NULL;

-- New PARENT role -- read-only over someone else's data, so it sits below
-- STUDENT's tier (hierarchy_level 5) rather than at it.
INSERT INTO app_roles (name, display_name, hierarchy_level, is_system_role, description, created_at, updated_at)
VALUES ('PARENT', 'Parent / Guardian', 6, false, 'Read-only self-service access to a linked ward''s records.', now(), now())
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('MY_WARD_ATTENDANCE_VIEW',  'View Ward Attendance',   'CURRICULUM',  'My Wards', 4, CURRENT_TIMESTAMP),
    ('MY_WARD_EXAM_RESULT_VIEW', 'View Ward Exam Results', 'EXAMINATION', 'My Wards', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Admin-facing Guardian record management (create a Guardian, link/unlink wards) --
-- distinct from creating the Guardian's *login account*, which reuses the existing
-- USER_VIEW-gated /user-management flow. Separate View/Manage tier per the
-- operation-wise permission mapping hard gate -- never reuse USER_VIEW for this.
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('GUARDIAN_VIEW',   'View Guardians',   'ADMISSION', 'Guardians', 2, CURRENT_TIMESTAMP),
    ('GUARDIAN_MANAGE', 'Manage Guardians', 'ADMISSION', 'Guardians', 2, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant directly to PARENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'PARENT'
  AND p.code IN ('MY_WARD_ATTENDANCE_VIEW', 'MY_WARD_EXAM_RESULT_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
