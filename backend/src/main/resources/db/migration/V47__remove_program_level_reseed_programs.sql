-- V47: Remove program_level column from programs and reseed programs as user-managed entities

-- Step 1: Drop the program_level column (no longer needed — Program IS the level)
ALTER TABLE programs DROP COLUMN IF EXISTS program_level;

-- Step 2: Seed programs as generic degree-level types first (required before migrating references)
INSERT INTO programs (name, code, duration_years, created_at, updated_at) VALUES
    ('Bachelor',    'BACHELOR',    4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Master',      'MASTER',      2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Diploma',     'DIPLOMA',     3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Certificate', 'CERTIFICATE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Doctoral',    'DOCTORAL',    3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Step 3: Migrate all data referencing the domain-specific seed programs (inserted by V45)
-- to the new generic degree-type programs: BSC_NURS → BACHELOR, MSC_NURS → MASTER, GNM → DIPLOMA.
-- Uses conditional logic so this is safe on databases that never had the old domain programs.
DO $$
DECLARE
    v_bachelor_id BIGINT;
    v_master_id   BIGINT;
    v_diploma_id  BIGINT;
    v_bsc_id      BIGINT;
    v_msc_id      BIGINT;
    v_gnm_id      BIGINT;
BEGIN
    SELECT id INTO v_bachelor_id FROM programs WHERE code = 'BACHELOR';
    SELECT id INTO v_master_id   FROM programs WHERE code = 'MASTER';
    SELECT id INTO v_diploma_id  FROM programs WHERE code = 'DIPLOMA';
    SELECT id INTO v_bsc_id      FROM programs WHERE code = 'BSC_NURS';
    SELECT id INTO v_msc_id      FROM programs WHERE code = 'MSC_NURS';
    SELECT id INTO v_gnm_id      FROM programs WHERE code = 'GNM';

    IF v_bsc_id IS NOT NULL THEN
        UPDATE courses                    SET program_id = v_bachelor_id WHERE program_id = v_bsc_id;
        UPDATE students                   SET program_id = v_bachelor_id WHERE program_id = v_bsc_id;
        UPDATE fee_structures             SET program_id = v_bachelor_id WHERE program_id = v_bsc_id;
        UPDATE student_fee_allocations    SET program_id = v_bachelor_id WHERE program_id = v_bsc_id;
        UPDATE enquiries                  SET program_id = v_bachelor_id WHERE program_id = v_bsc_id;
        UPDATE agent_commission_guidelines SET program_id = v_bachelor_id WHERE program_id = v_bsc_id;
    END IF;

    IF v_msc_id IS NOT NULL THEN
        UPDATE courses                    SET program_id = v_master_id WHERE program_id = v_msc_id;
        UPDATE students                   SET program_id = v_master_id WHERE program_id = v_msc_id;
        UPDATE fee_structures             SET program_id = v_master_id WHERE program_id = v_msc_id;
        UPDATE student_fee_allocations    SET program_id = v_master_id WHERE program_id = v_msc_id;
        UPDATE enquiries                  SET program_id = v_master_id WHERE program_id = v_msc_id;
        UPDATE agent_commission_guidelines SET program_id = v_master_id WHERE program_id = v_msc_id;
    END IF;

    IF v_gnm_id IS NOT NULL THEN
        UPDATE courses                    SET program_id = v_diploma_id WHERE program_id = v_gnm_id;
        UPDATE students                   SET program_id = v_diploma_id WHERE program_id = v_gnm_id;
        UPDATE fee_structures             SET program_id = v_diploma_id WHERE program_id = v_gnm_id;
        UPDATE student_fee_allocations    SET program_id = v_diploma_id WHERE program_id = v_gnm_id;
        UPDATE enquiries                  SET program_id = v_diploma_id WHERE program_id = v_gnm_id;
        UPDATE agent_commission_guidelines SET program_id = v_diploma_id WHERE program_id = v_gnm_id;
    END IF;
END $$;

-- Step 4: Remove program_departments entries for old domain-specific programs
DELETE FROM program_departments WHERE program_id IN (
    SELECT id FROM programs WHERE code IN ('BSC_NURS', 'MSC_NURS', 'GNM')
);

-- Step 5: Delete old domain-specific seed programs (all FK references migrated above)
DELETE FROM programs WHERE code IN ('BSC_NURS', 'MSC_NURS', 'GNM');
