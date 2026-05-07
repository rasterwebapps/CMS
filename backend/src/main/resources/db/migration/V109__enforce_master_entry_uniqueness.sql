-- V109: Enforce master-entry uniqueness at the database layer.
-- Service-layer validation now blocks duplicates before save; these indexes protect against
-- concurrent requests and any non-API writes. Existing duplicate display names are preserved
-- by appending the record id to later duplicates before creating the unique indexes.

-- Agents: duplicate names are not allowed (case/space-insensitive).
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM agents
    WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE agents a
SET name = a.name || ' #' || a.id
FROM ranked r
WHERE a.id = r.id AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_agents_name_ci
    ON agents (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';

-- Departments: globally unique names and codes.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM departments WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE departments d SET name = d.name || ' #' || d.id FROM ranked r WHERE d.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(code)) ORDER BY id) AS rn
    FROM departments WHERE code IS NOT NULL AND trim(code) <> ''
)
UPDATE departments d SET code = d.code || '_' || d.id FROM ranked r WHERE d.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_departments_name_ci
    ON departments (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_departments_code_ci
    ON departments (lower(trim(code)))
    WHERE code IS NOT NULL AND trim(code) <> '';

-- Programs: globally unique names and codes.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM programs WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE programs p SET name = p.name || ' #' || p.id FROM ranked r WHERE p.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(code)) ORDER BY id) AS rn
    FROM programs WHERE code IS NOT NULL AND trim(code) <> ''
)
UPDATE programs p SET code = p.code || '_' || p.id FROM ranked r WHERE p.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_programs_name_ci
    ON programs (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_programs_code_ci
    ON programs (lower(trim(code)))
    WHERE code IS NOT NULL AND trim(code) <> '';

-- Courses: globally unique names and codes.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM courses WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE courses c SET name = c.name || ' #' || c.id FROM ranked r WHERE c.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(code)) ORDER BY id) AS rn
    FROM courses WHERE code IS NOT NULL AND trim(code) <> ''
)
UPDATE courses c SET code = c.code || '_' || c.id FROM ranked r WHERE c.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_courses_name_ci
    ON courses (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_courses_code_ci
    ON courses (lower(trim(code)))
    WHERE code IS NOT NULL AND trim(code) <> '';

-- Academic years: globally unique name.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM academic_years WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE academic_years a SET name = a.name || ' #' || a.id FROM ranked r WHERE a.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_academic_years_name_ci
    ON academic_years (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';

-- Referral types: globally unique names and codes.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM referral_types WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE referral_types rt SET name = rt.name || ' #' || rt.id FROM ranked r WHERE rt.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(code)) ORDER BY id) AS rn
    FROM referral_types WHERE code IS NOT NULL AND trim(code) <> ''
)
UPDATE referral_types rt SET code = rt.code || '_' || rt.id FROM ranked r WHERE rt.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_referral_types_name_ci
    ON referral_types (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_referral_types_code_ci
    ON referral_types (lower(trim(code)))
    WHERE code IS NOT NULL AND trim(code) <> '';

-- Faculty: globally unique employee code and email.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(employee_code)) ORDER BY id) AS rn
    FROM faculty WHERE employee_code IS NOT NULL AND trim(employee_code) <> ''
)
UPDATE faculty f SET employee_code = f.employee_code || '_' || f.id FROM ranked r WHERE f.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(email)) ORDER BY id) AS rn
    FROM faculty WHERE email IS NOT NULL AND trim(email) <> ''
)
UPDATE faculty f SET email = 'duplicate-' || f.id || '-' || f.email FROM ranked r WHERE f.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_faculty_employee_code_ci
    ON faculty (lower(trim(employee_code)))
    WHERE employee_code IS NOT NULL AND trim(employee_code) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_faculty_email_ci
    ON faculty (lower(trim(email)))
    WHERE email IS NOT NULL AND trim(email) <> '';

-- Labs: names are unique within a department.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY department_id, lower(trim(name)) ORDER BY id) AS rn
    FROM labs WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE labs l SET name = l.name || ' #' || l.id FROM ranked r WHERE l.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_labs_department_name_ci
    ON labs (department_id, lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';

-- Communities: globally unique names and codes.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM communities WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE communities c SET name = c.name || ' #' || c.id FROM ranked r WHERE c.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(code)) ORDER BY id) AS rn
    FROM communities WHERE code IS NOT NULL AND trim(code) <> ''
)
UPDATE communities c SET code = c.code || '_' || c.id FROM ranked r WHERE c.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_communities_name_ci
    ON communities (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_communities_code_ci
    ON communities (lower(trim(code)))
    WHERE code IS NOT NULL AND trim(code) <> '';

-- Blood groups: globally unique names and codes.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(name)) ORDER BY id) AS rn
    FROM blood_groups WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE blood_groups b SET name = b.name || ' #' || b.id FROM ranked r WHERE b.id = r.id AND r.rn > 1;
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(code)) ORDER BY id) AS rn
    FROM blood_groups WHERE code IS NOT NULL AND trim(code) <> ''
)
UPDATE blood_groups b SET code = b.code || '_' || b.id FROM ranked r WHERE b.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_blood_groups_name_ci
    ON blood_groups (lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS ux_blood_groups_code_ci
    ON blood_groups (lower(trim(code)))
    WHERE code IS NOT NULL AND trim(code) <> '';

-- Equipment: asset codes are globally unique when provided.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(trim(asset_code)) ORDER BY id) AS rn
    FROM equipment WHERE asset_code IS NOT NULL AND trim(asset_code) <> ''
)
UPDATE equipment e SET asset_code = e.asset_code || '_' || e.id FROM ranked r WHERE e.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_equipment_asset_code_ci
    ON equipment (lower(trim(asset_code)))
    WHERE asset_code IS NOT NULL AND trim(asset_code) <> '';

-- Semesters: names are unique within an academic year.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY academic_year_id, lower(trim(name)) ORDER BY id) AS rn
    FROM semesters WHERE name IS NOT NULL AND trim(name) <> ''
)
UPDATE semesters s SET name = s.name || ' #' || s.id FROM ranked r WHERE s.id = r.id AND r.rn > 1;
CREATE UNIQUE INDEX IF NOT EXISTS ux_semesters_academic_year_name_ci
    ON semesters (academic_year_id, lower(trim(name)))
    WHERE name IS NOT NULL AND trim(name) <> '';

