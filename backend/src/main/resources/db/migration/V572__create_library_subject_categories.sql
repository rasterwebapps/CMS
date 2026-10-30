-- ============================================================
-- V572: Library Subject Category Master
--   1. Create library_subject_categories master table
--   2. Seed standard 19 subject categories
--   3. Harvest any distinct legacy custom categories from library_books
--   4. Add subject_category_id FK to library_books and index
--   5. Backfill subject_category_id from existing subject_category values
--   6. Seed RBAC permissions & role mappings
-- ============================================================

-- 1. MASTER TABLE
CREATE TABLE library_subject_categories (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    code        VARCHAR(50)     NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_lib_subj_cat_name UNIQUE (name),
    CONSTRAINT uq_lib_subj_cat_code UNIQUE (code)
);

CREATE INDEX idx_lib_subj_cat_active ON library_subject_categories (is_active);

-- 2. SEED INITIAL STANDARD 19 CATEGORIES
INSERT INTO library_subject_categories (name, code) VALUES
    ('Anatomy & Physiology',     'ANAT_PHYS'),
    ('Biochemistry',             'BIOCHEM'),
    ('Microbiology',             'MICROBIO'),
    ('Pathology',                'PATH'),
    ('Pharmacology',             'PHARM'),
    ('Nursing Fundamentals',     'NURS_FUND'),
    ('Medical-Surgical Nursing', 'MED_SURG_NURS'),
    ('Obstetrics & Gynaecology', 'OBG'),
    ('Paediatric Nursing',       'PAED_NURS'),
    ('Mental Health Nursing',    'MENTAL_HEALTH'),
    ('Community Health Nursing', 'COMM_HEALTH'),
    ('Midwifery',                'MIDWIFERY'),
    ('Nutrition',                'NUTRITION'),
    ('Psychology',               'PSYCHOLOGY'),
    ('English',                  'ENGLISH'),
    ('Computer Science',         'COMP_SCI'),
    ('Management',               'MANAGEMENT'),
    ('Research',                 'RESEARCH'),
    ('General',                  'GENERAL')
ON CONFLICT (name) DO NOTHING;

-- 3. HARVEST ANY CUSTOM / LEGACY CATEGORIES PRESENT IN EXISTING BOOKS
WITH custom_cats AS (
    SELECT DISTINCT btrim(subject_category) AS cat_name
    FROM library_books
    WHERE subject_category IS NOT NULL
      AND btrim(subject_category) <> ''
      AND NOT EXISTS (
          SELECT 1 FROM library_subject_categories sc
          WHERE lower(btrim(sc.name)) = lower(btrim(library_books.subject_category))
      )
),
custom_with_codes AS (
    SELECT cat_name,
           upper(regexp_replace(btrim(cat_name), '[^A-Za-z0-9]+', '_', 'g')) AS base_code,
           ROW_NUMBER() OVER (ORDER BY cat_name) AS rn
    FROM custom_cats
)
INSERT INTO library_subject_categories (name, code)
SELECT cat_name,
       CASE WHEN length(base_code) > 0 THEN substring(base_code from 1 for 40) || '_' || rn
            ELSE 'CAT_' || rn END
FROM custom_with_codes
ON CONFLICT (name) DO NOTHING;

-- 4. ADD FK COLUMN TO library_books
ALTER TABLE library_books
    ADD COLUMN subject_category_id BIGINT REFERENCES library_subject_categories(id);

CREATE INDEX idx_library_books_subject_category_id ON library_books (subject_category_id);

-- 5. BACKFILL FOREIGN KEY ON EXISTING BOOKS
UPDATE library_books b
   SET subject_category_id = sc.id
  FROM library_subject_categories sc
 WHERE b.subject_category IS NOT NULL
   AND lower(btrim(b.subject_category)) = lower(btrim(sc.name));

-- 6. PERMISSIONS & ROLE MAPPINGS
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('LIBRARY_SUBJECT_CATEGORY_VIEW',   'View Library Subject Categories',   'LIBRARY', 'Subject Categories', 4, CURRENT_TIMESTAMP),
    ('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'Manage Library Subject Categories', 'LIBRARY', 'Subject Categories', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign VIEW to roles holding LIBRARY_CATALOGUE_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_CATALOGUE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_SUBJECT_CATEGORY_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- Auto-assign MANAGE to roles holding LIBRARY_CATALOGUE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_CATALOGUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_SUBJECT_CATEGORY_MANAGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND p.code IN ('LIBRARY_SUBJECT_CATEGORY_VIEW', 'LIBRARY_SUBJECT_CATEGORY_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
