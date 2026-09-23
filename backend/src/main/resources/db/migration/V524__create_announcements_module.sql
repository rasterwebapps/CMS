-- ============================================================
-- V524: Announcements module (OC-255) -- institution-wide, not Parent-Portal-only.
-- ============================================================
-- Per the Parent Portal specialist-review round (2026-09-17): a general-purpose
-- announcements feature, first consumed by the Parent Portal (guardians) but built
-- for every role from the start. Authorship is ADMIN-tier only for this slice (no
-- faculty-authored announcements yet). Always published on create -- no draft
-- workflow in this first slice, matching the "admin-only, no complex workflow" scope.
--
-- Audience targeting: ROLE (matches app_roles.name), COHORT (matches students.cohort_id
-- for the viewer or their ward), SECTION (matches a cohort_section via the
-- batch_students -> batches.cohort_section_id path -- cohort_sections itself carries
-- no direct student membership), or ALL (broadcast to everyone with ANNOUNCEMENT_VIEW).
-- One announcement can carry multiple audience rows (e.g. two different cohorts).
--
-- created_by / announcement_reads.user_id store the keycloak preferred_username, the
-- same convention notification_dismissals.user_id and CurrentUserResolver already use
-- (see V287__create_notifications.sql), not a numeric FK -- consistent with how
-- payment_receipts.collected_by also stores a plain identifying string rather than a FK.
-- ============================================================

CREATE TABLE announcements (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    body         TEXT NOT NULL,
    created_by   VARCHAR(255) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE announcement_audiences (
    id               BIGSERIAL PRIMARY KEY,
    announcement_id  BIGINT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    audience_type    VARCHAR(10) NOT NULL CHECK (audience_type IN ('ROLE', 'COHORT', 'SECTION', 'ALL')),
    audience_ref_id  BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- ALL rows carry no ref id and are guarded against duplication in the service layer
    -- (a partial unique index can't cover "at most one ALL row" cleanly alongside the
    -- ROLE/COHORT/SECTION rows' own ref-id-scoped uniqueness in one constraint).
    CONSTRAINT chk_announcement_audience_ref CHECK (
        (audience_type = 'ALL' AND audience_ref_id IS NULL) OR
        (audience_type <> 'ALL' AND audience_ref_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_announcement_audience_targeted
    ON announcement_audiences (announcement_id, audience_type, audience_ref_id)
    WHERE audience_type <> 'ALL';

CREATE INDEX idx_announcement_audiences_lookup ON announcement_audiences (audience_type, audience_ref_id);
CREATE INDEX idx_announcement_audiences_announcement ON announcement_audiences (announcement_id);

CREATE TABLE announcement_reads (
    id              BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id         VARCHAR(255) NOT NULL,
    read_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_announcement_read UNIQUE (announcement_id, user_id)
);

CREATE INDEX idx_announcement_reads_user ON announcement_reads (user_id);

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('ANNOUNCEMENT_MANAGE', 'Manage Announcements', 'COMMUNICATION', 'Announcements', 2, CURRENT_TIMESTAMP),
    ('ANNOUNCEMENT_VIEW',   'View My Announcements', 'COMMUNICATION', 'Announcements', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ANNOUNCEMENT_MANAGE: admin-tier only (ADMIN -- full college-level administrator;
-- collegeadmin is deliberately excluded here, its role is scoped to admission-workflow
-- operations per V123, not general institution-wide communication).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code = 'ANNOUNCEMENT_MANAGE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- ANNOUNCEMENT_VIEW: every self-service consuming role.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('STUDENT', 'FACULTY', 'PARENT', 'ADMIN')
  AND p.code = 'ANNOUNCEMENT_VIEW'
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
