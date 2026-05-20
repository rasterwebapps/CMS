-- V152: Add Country master and link existing India States to it.
--
-- Creates location_countries as the top-level table in the Country → State → District
-- hierarchy. All existing india_states are assigned to India (iso_code = 'IN').
-- Uniqueness constraints on india_states are changed from global to per-country scope.

-- (1) Country master table
CREATE TABLE location_countries (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(150) NOT NULL UNIQUE,
    iso_code   VARCHAR(3)   NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_location_countries_is_active ON location_countries(is_active);

-- (2) Seed India as the founding country (id = 1 guaranteed by explicit insert)
INSERT INTO location_countries (id, name, iso_code) VALUES (1, 'India', 'IN');
SELECT setval('location_countries_id_seq', 1, true);

-- (3) Add country_id FK to india_states
ALTER TABLE india_states ADD COLUMN country_id BIGINT;
UPDATE india_states SET country_id = 1;
ALTER TABLE india_states ALTER COLUMN country_id SET NOT NULL;
ALTER TABLE india_states
    ADD CONSTRAINT fk_india_states_country
    FOREIGN KEY (country_id) REFERENCES location_countries(id);

-- (4) Replace global unique constraints with per-country unique constraints
ALTER TABLE india_states DROP CONSTRAINT india_states_name_key;
ALTER TABLE india_states DROP CONSTRAINT india_states_code_key;
ALTER TABLE india_states ADD CONSTRAINT uq_states_country_name UNIQUE (country_id, name);
ALTER TABLE india_states ADD CONSTRAINT uq_states_country_code UNIQUE (country_id, code);

CREATE INDEX idx_india_states_country_id ON india_states(country_id);

-- (5) Add COUNTRY_MANAGE permission and grant to admin roles
INSERT INTO permissions (code, display_name, category)
SELECT 'COUNTRY_MANAGE', 'Manage Countries', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'COUNTRY_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code = 'COUNTRY_MANAGE'
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

