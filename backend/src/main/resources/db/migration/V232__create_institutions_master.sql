-- V232: Create institutions master table (known sister-concern colleges of
-- SKSCON) and seed INSTITUTION_VIEW / INSTITUTION_MANAGE permissions.

CREATE TABLE institutions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_institutions_name UNIQUE (name),
    CONSTRAINT uq_institutions_code UNIQUE (code)
);

INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('INSTITUTION_VIEW',   'View Institutions',   'MASTER', CURRENT_TIMESTAMP),
    ('INSTITUTION_MANAGE', 'Manage Institutions', 'MASTER', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN: same roles that manage Staff
-- Referrers, since this master backs the institution dropdown on that form.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INSTITUTION_VIEW', 'INSTITUTION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
