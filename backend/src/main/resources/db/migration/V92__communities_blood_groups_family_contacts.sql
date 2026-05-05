-- V92: Add communities and blood_groups master tables, extend student family info.
--
-- 1. Communities master table
CREATE TABLE communities (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 2. Blood groups master table
CREATE TABLE blood_groups (
    id         BIGSERIAL   PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 3. Seed default community values (same codes as old enum)
INSERT INTO communities (name, code, description) VALUES
    ('Scheduled Caste',          'SC',     'Scheduled Caste'),
    ('Scheduled Tribe',          'ST',     'Scheduled Tribe'),
    ('Backward Caste',           'BC',     'Backward Caste'),
    ('Most Backward Caste',      'MBC',    'Most Backward Caste'),
    ('Denotified Communities',   'DNC',    'Denotified Communities'),
    ('Open Category',            'OC',     'Open / General Category'),
    ('Economically Weaker Section', 'EWS', 'Economically Weaker Section'),
    ('Others',                   'OTHERS', 'Others');

-- 4. Seed default blood group values (human-readable codes)
INSERT INTO blood_groups (name, code) VALUES
    ('A Positive',  'A+'),
    ('A Negative',  'A-'),
    ('B Positive',  'B+'),
    ('B Negative',  'B-'),
    ('O Positive',  'O+'),
    ('O Negative',  'O-'),
    ('AB Positive', 'AB+'),
    ('AB Negative', 'AB-');

-- 5. Migrate community_category column from old enum names to new codes
--    (Most are identical; ensure 'OTHERS' stays as 'OTHERS')
-- No-op for most values as codes match enum names.

-- 6. Migrate blood_group column from enum names to human-readable codes
UPDATE students SET blood_group = 'A+'  WHERE blood_group = 'A_POSITIVE';
UPDATE students SET blood_group = 'A-'  WHERE blood_group = 'A_NEGATIVE';
UPDATE students SET blood_group = 'B+'  WHERE blood_group = 'B_POSITIVE';
UPDATE students SET blood_group = 'B-'  WHERE blood_group = 'B_NEGATIVE';
UPDATE students SET blood_group = 'O+'  WHERE blood_group = 'O_POSITIVE';
UPDATE students SET blood_group = 'O-'  WHERE blood_group = 'O_NEGATIVE';
UPDATE students SET blood_group = 'AB+' WHERE blood_group = 'AB_POSITIVE';
UPDATE students SET blood_group = 'AB-' WHERE blood_group = 'AB_NEGATIVE';

-- 7. Add new family contact columns to students
ALTER TABLE students ADD COLUMN IF NOT EXISTS father_phone VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS father_email VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS mother_phone VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS mother_email VARCHAR(255);

