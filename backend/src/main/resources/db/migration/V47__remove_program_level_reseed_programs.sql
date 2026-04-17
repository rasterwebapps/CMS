-- V47: Remove program_level column from programs and reseed programs as user-managed entities

-- Step 1: Drop the program_level column (no longer needed — Program IS the level)
ALTER TABLE programs DROP COLUMN IF EXISTS program_level;

-- Step 2: Remove domain-specific seed programs from the initial V45 seed data.
-- These codes (BSC_NURS, MSC_NURS, GNM) were only inserted by V45__insert_seed_data.sql.
-- Any programs added by users after initial setup will not be affected by this deletion.
DELETE FROM program_departments WHERE program_id IN (
    SELECT id FROM programs WHERE code IN ('BSC_NURS', 'MSC_NURS', 'GNM')
);
DELETE FROM programs WHERE code IN ('BSC_NURS', 'MSC_NURS', 'GNM');

-- Step 3: Seed programs as generic degree-level types
INSERT INTO programs (name, code, duration_years, created_at, updated_at) VALUES
    ('Bachelor',    'BACHELOR',    4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Master',      'MASTER',      2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Diploma',     'DIPLOMA',     3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Certificate', 'CERTIFICATE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Doctoral',    'DOCTORAL',    3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
