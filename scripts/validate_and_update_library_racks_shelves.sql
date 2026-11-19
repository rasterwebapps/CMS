-- =============================================================================
-- Script: validate_and_update_library_racks_shelves.sql
-- Path:   /home/raster/Projects/OneCMSV2/scripts/validate_and_update_library_racks_shelves.sql
--
-- EXECUTED on Local (cms-postgres / cms_16nov) on 2026-11-16 (~17:11 IST): COMMIT, psql exit 0.
-- EXECUTED on Live 209 (skscms209-postgres-1 / cms) on 2026-11-16 (~17:40 IST): COMMIT, psql exit 0.
-- Pre-run Live Backup: /docker_data/backups/cms_b4_update_rackandshelf_1611261739.backup (2.3M)
--
-- Purpose:
--   1. Align library_racks (25 racks: C01..C25) and library_shelves (104 shelves:
--      S1..S4 for C01..C17 & C20..C25; S1..S6 for C18 & C19) as per
--      /home/raster/SANKAR/OneCMS/Rack_and_shelves.xls.
--   2. Preserve existing library_racks.description values on C01..C21.
--   3. Use 'C02-S1,S2,S3,S4' for C02 rack code and set is_active = true on all
--      25 racks and 104 shelves.
--   4. Re-map library_books.shelf_id from the legacy V254 inverted shelves
--      (id = 6..93, named C1..C18 under old R1..R7 racks) to their true
--      (Rack C01..C18, Shelf S1..S6) records, unassign the catch-all
--      'Unassigned' shelf (id = 94 -> shelf_id = NULL), and delete the obsolete
--      shelves safely.
--   5. Validate all rack, shelf, book, and transfer counts before COMMIT.
--
-- Usage:
--   Local:
--     docker exec -i cms-postgres psql -U cms -d cms_16nov < /home/raster/Projects/OneCMSV2/scripts/validate_and_update_library_racks_shelves.sql
--   Live Server:
--     docker exec -i <live-postgres-container> psql -U <db-user> -d <db-name> < /home/raster/Projects/OneCMSV2/scripts/validate_and_update_library_racks_shelves.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- =============================================================================
-- STEP 0: Capture Pre-Execution Baseline Counts for Validation
-- =============================================================================
CREATE TEMP TABLE _baseline_counts ON COMMIT DROP AS
SELECT
    (SELECT COUNT(*) FROM library_books) AS total_books_before,
    (SELECT COUNT(*) FROM library_books WHERE shelf_id IS NOT NULL) AS assigned_books_before,
    (SELECT COUNT(*) FROM library_books WHERE shelf_id IS NULL) AS null_books_before,
    (SELECT COUNT(*) FROM library_books b JOIN library_shelves s ON b.shelf_id = s.id WHERE s.id = 94 AND s.name = 'Unassigned') AS unassigned_shelf_books_before;

\echo '=== STEP 0: Pre-Execution Baseline Book Counts ==='
SELECT * FROM _baseline_counts;

-- =============================================================================
-- STEP 1: Rack C01 & Its Shelves (Reduce 7 shelves -> 4 active shelves S1..S4)
--         (Idempotent: safe to run whether C01..C14 were already renamed or still
--          have their V254 names E, R1..R7, Unassigned)
-- =============================================================================
\echo '=== STEP 1: Updating Rack C01 and its 4 shelves (S1..S4) ==='

-- 1a-pre. If racks id = 1..9 still carry V254 names (E, R1..R7, Unassigned),
--         rename them in-place to C01, C03..C09, C14 to match Live 209 / Local.
UPDATE library_racks
SET name = CASE id
             WHEN 1 THEN 'C01'
             WHEN 2 THEN 'C03'
             WHEN 3 THEN 'C04'
             WHEN 4 THEN 'C05'
             WHEN 5 THEN 'C06'
             WHEN 6 THEN 'C07'
             WHEN 7 THEN 'C08'
             WHEN 8 THEN 'C09'
             WHEN 9 THEN 'C14'
           END,
    code = CASE id
             WHEN 1 THEN 'C01-S1,S2,S3,S4'
             WHEN 2 THEN 'C03-S1,S2,S3,S4'
             WHEN 3 THEN 'C04-S1,S2,S3,S4'
             WHEN 4 THEN 'C05-S1,S2,S3,S4'
             WHEN 5 THEN 'C06-S1,S2,S3,S4'
             WHEN 6 THEN 'C07-S1,S2,S3,S4'
             WHEN 7 THEN 'C08-S1,S2,S3,S4'
             WHEN 8 THEN 'C09-S1,S2,S3,S4'
             WHEN 9 THEN 'C14-S1,S2,S3,S4'
           END,
    updated_at = NOW()
WHERE id BETWEEN 1 AND 9
  AND name IN ('E', 'R1', 'R2', 'R3', 'R4', 'R5', 'R6', 'R7', 'Unassigned');

-- Re-point any book still on legacy shelf id = 5 (under rack_id = 1) to shelf id = 4 (S4)
UPDATE library_books
SET shelf_id = 4
WHERE shelf_id = 5
  AND EXISTS (SELECT 1 FROM library_shelves WHERE id = 4 AND rack_id = 1);

-- 1a. Ensure Rack C01 has expected code and is_active = true (preserving description)
UPDATE library_racks
SET code       = 'C01-S1,S2,S3,S4',
    is_active  = true,
    updated_at = NOW()
WHERE id = 1
  AND name = 'C01';

-- 1b. Delete the 3 extra shelves under Rack C01:
--     id = 5 ('4' inactive), id = 96 ('05' active), id = 97 ('06' active).
--     Deleting id = 5 first frees up code = '4' under rack_id = 1.
DELETE FROM library_shelves s
WHERE s.rack_id = 1
  AND s.id IN (5, 96, 97)
  AND NOT EXISTS (SELECT 1 FROM library_books b WHERE b.shelf_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM library_book_shelf_transfers t WHERE t.old_shelf_id = s.id OR t.new_shelf_id = s.id);

-- 1c. Update C01's 4 shelves (id = 1, 2, 3, 4) in-place to S1..S4 (codes 1..4)
UPDATE library_shelves
SET name       = CASE id
                   WHEN 1 THEN 'S1'
                   WHEN 2 THEN 'S2'
                   WHEN 3 THEN 'S3'
                   WHEN 4 THEN 'S4'
                 END,
    code       = CASE id
                   WHEN 1 THEN '1'
                   WHEN 2 THEN '2'
                   WHEN 3 THEN '3'
                   WHEN 4 THEN '4'
                 END,
    is_active  = true,
    updated_at = NOW()
WHERE rack_id = 1
  AND id IN (1, 2, 3, 4);

-- =============================================================================
-- STEP 2: Align All 25 Racks (C01..C25) in library_racks
--         - Updates C01..C21 codes (fixing C20-S and C21-S, keeping C02-S1,S2,S3,S4)
--         - Preserves existing description column on C01..C21
--         - Inserts missing racks C22..C25
-- =============================================================================
\echo '=== STEP 2: Aligning all 25 Racks (C01..C25) in library_racks ==='

CREATE TEMP TABLE _target_racks (
    excel_rack_no INT PRIMARY KEY,
    rack_name     VARCHAR(200) NOT NULL,
    rack_code     VARCHAR(50)  NOT NULL,
    rack_desc     TEXT,
    rack_active   BOOLEAN      NOT NULL,
    shelf_count   INT          NOT NULL
) ON COMMIT DROP;

INSERT INTO _target_racks (excel_rack_no, rack_name, rack_code, rack_desc, rack_active, shelf_count) VALUES
    ( 1, 'C01', 'C01-S1,S2,S3,S4',       'ANATOMY & PHYSIOLOGY',                                                                                              true, 4),
    ( 2, 'C02', 'C02-S1,S2,S3,S4',       'NURSING FOUNDATION',                                                                                                true, 4),
    ( 3, 'C03', 'C03-S1,S2,S3,S4',       E'S1-SOCIOLOGY\nS2,S3,S4- PSYCHOLOGY & SOCIOLOGY',                                                                   true, 4),
    ( 4, 'C04', 'C04-S1,S2,S3,S4',       E'S1 & S2 COMMUNITY HEALTH NURSING-1\nS3- NUTRITION- 3\nS4- FIRST AID -4',                                           true, 4),
    ( 5, 'C05', 'C05-S1,S2,S3,S4',       'MEDICAL & SURGICAL NURSING 1 & 2',                                                                                  true, 4),
    ( 6, 'C06', 'C06-S1,S2,S3,S4',       E'S1-FUNDAMENTALS OF NURSING\nS2,S3,S4- SURGERY',                                                                    true, 4),
    ( 7, 'C07', 'C07-S1,S2,S3,S4',       E'S1 & S2- PEDIATRIC NURSING\nS3 & S4- MENTAL HEALTH NURSING',                                                       true, 4),
    ( 8, 'C08', 'C08-S1,S2,S3,S4',       E'S1 & S2- MICROBIOLOGY\nS3 & S4- UROLOGY & NEUROLOGY',                                                              true, 4),
    ( 9, 'C09', 'C09-S1,S2,S3,S4',       E'S1 & S2- MEDICINE\nS3 & S4- OBSTETRICS & GYNAECOLOGY',                                                             true, 4),
    (10, 'C10', 'C10-S1,S2,S3,S4',       E'S2 & S2- GYNAECOLOGY & MIDWIFERY\nS3 & S4- OBSTETRICS & GYNAECOLOGY',                                              true, 4),
    (11, 'C11', 'C11-S1,S2,S3,S4',       E'S1 & S2-REFERENCE\nS3 & S4- NURSING RESEARCH',                                                                     true, 4),
    (12, 'C12', 'C12-S1,S2,S3,S4',       E'S1 & S2- PHARMACOLOGY\nS3 & S4- DICTIONARY',                                                                       true, 4),
    (13, 'C13', 'C13-S1,S2,S3,S4',       E'S2, S2 & S3- COMPUTER\nS4- INTERSHIP',                                                                             true, 4),
    (14, 'C14', 'C14-S1,S2,S3,S4',       E'S1-NIGHTNGLE NURSING TIMES\nS2-REGISTERS',                                                                         true, 4),
    (15, 'C15', 'C15-S1,S2,S3,S4',       E'S1- MEDICAL SURGICAL NURSING\nS2 & S3 MEDICAL SURGICAL NURSING-1\nS4- NURSING MANAGEMENT',                         true, 4),
    (16, 'C16', 'C16-S1,S2,S3,S4',       'S1-4-HEALTH EDUCATION',                                                                                             true, 4),
    (17, 'C17', 'C17-S1,S2,S3,S4',       'LIBRARY OFFICE NOTES & BOOKS',                                                                                      true, 4),
    (18, 'C18', 'C18-S1,S2,S3,S4,S5,S6', E'S1 & S2- COMMUNITY\nS3- MEDICINE & FORENSIC\nS4- MEDICINE\nS5- NURSING FOUNDATION\nS6- ANATOMY & PHYSIOLOGY',      true, 6),
    (19, 'C19', 'C19-S1,S2,S3,S4,S5,S6', E'S1-ENGLISH\nS2-PROFESSIONAL & ETHICS\nS3-BIO CHEMISTRY\nS4- PATHOLOGY & GENETICS\nS5 & S6- CHILS HEALTH NURSING', true, 6),
    (20, 'C20', 'C20-S1,S2,S3,S4',       'COMPLEMENTRY BOOKS',                                                                                                true, 4),
    (21, 'C21', 'C21-S1,S2,S3,S4',       'JOURNALS',                                                                                                          true, 4),
    (22, 'C22', 'C22-S1,S2,S3,S4',       NULL,                                                                                                                true, 4),
    (23, 'C23', 'C23-S1,S2,S3,S4',       NULL,                                                                                                                true, 4),
    (24, 'C24', 'C24-S1,S2,S3,S4',       NULL,                                                                                                                true, 4),
    (25, 'C25', 'C25-S1,S2,S3,S4',       NULL,                                                                                                                true, 4);

-- 2a. Update existing racks (C01..C21), preserving non-empty description and populating missing description
UPDATE library_racks r
SET code        = tr.rack_code,
    description = COALESCE(NULLIF(r.description, ''), tr.rack_desc),
    is_active   = tr.rack_active,
    updated_at  = NOW()
FROM _target_racks tr
WHERE r.name = tr.rack_name
  AND r.library_id = (SELECT id FROM libraries WHERE code = 'MAIN')
  AND (r.code <> tr.rack_code OR r.is_active <> tr.rack_active OR (r.description IS NULL AND tr.rack_desc IS NOT NULL));

-- 2b. Insert missing racks (e.g. C02, C10..C13, C15..C25)
INSERT INTO library_racks (library_id, name, code, description, is_active, created_at, updated_at)
SELECT
    (SELECT id FROM libraries WHERE code = 'MAIN'),
    tr.rack_name,
    tr.rack_code,
    tr.rack_desc,
    tr.rack_active,
    NOW(),
    NOW()
FROM _target_racks tr
WHERE NOT EXISTS (
    SELECT 1 FROM library_racks r
    WHERE r.library_id = (SELECT id FROM libraries WHERE code = 'MAIN')
      AND r.name = tr.rack_name
)
ORDER BY tr.excel_rack_no;

-- =============================================================================
-- STEP 3: Create / Update All 104 Target Shelves (S1..S4 / S1..S6)
-- =============================================================================
\echo '=== STEP 3: Creating / updating all 104 target shelves across C01..C25 ==='

-- 3a. Update Rack C02's existing shelf (id = 95, name = '01', code = '04') in-place
--     to S1 (code = '1', is_active = true) so its 5 books remain linked to C02 -> S1.
UPDATE library_shelves s
SET name       = 'S1',
    code       = '1',
    is_active  = true,
    updated_at = NOW()
FROM library_racks r
WHERE s.rack_id = r.id
  AND r.name = 'C02'
  AND s.id = 95;

-- 3b. Insert all missing target shelves (S1..S4 for 23 racks, S1..S6 for C18 & C19)
INSERT INTO library_shelves (rack_id, name, code, is_active, created_at, updated_at)
SELECT
    r.id AS rack_id,
    'S' || gs.shelf_no::text AS name,
    gs.shelf_no::text AS code,
    true AS is_active,
    NOW(),
    NOW()
FROM _target_racks tr
JOIN library_racks r
  ON r.name = tr.rack_name
 AND r.library_id = (SELECT id FROM libraries WHERE code = 'MAIN')
CROSS JOIN LATERAL generate_series(1, tr.shelf_count) AS gs(shelf_no)
ON CONFLICT (rack_id, name) DO UPDATE
SET code       = EXCLUDED.code,
    is_active  = true,
    updated_at = NOW();

-- =============================================================================
-- STEP 4: Re-map library_books.shelf_id from Legacy Inverted Shelves (id = 6..94)
--         - Legacy shelves id = 6..93 have names 'C1'..'C18' under old racks:
--             rack_id = 2 (was R1) -> S1
--             rack_id = 3 (was R2) -> S2
--             rack_id = 4 (was R3) -> S3
--             rack_id = 5 (was R4) -> S4
--             rack_id = 6 (was R5) & C18 -> S5
--             rack_id = 7 (was R6) & C18 -> S6
--             rack_id IN (6,7,8) & not C18 (4-shelf cupboards) -> S4
--         - Legacy shelf id = 94 ('Unassigned' under rack_id = 9) -> shelf_id = NULL
-- =============================================================================
\echo '=== STEP 4: Re-mapping library_books from legacy inverted shelves (id = 6..94) ==='

CREATE TEMP TABLE _legacy_shelf_mapping ON COMMIT DROP AS
SELECT
    s_old.id AS old_shelf_id,
    s_old.rack_id AS old_rack_id,
    s_old.name AS old_shelf_name,
    r_new.id AS new_rack_id,
    r_new.name AS new_rack_name,
    s_new.id AS new_shelf_id,
    s_new.name AS new_shelf_name
FROM library_shelves s_old
JOIN library_racks r_new
  ON r_new.library_id = (SELECT id FROM libraries WHERE code = 'MAIN')
 AND r_new.name = 'C' || LPAD(SUBSTRING(s_old.name FROM 2), 2, '0')
JOIN library_shelves s_new
  ON s_new.rack_id = r_new.id
 AND s_new.name = CASE
                    WHEN s_old.rack_id = 2 THEN 'S1'
                    WHEN s_old.rack_id = 3 THEN 'S2'
                    WHEN s_old.rack_id = 4 THEN 'S3'
                    WHEN s_old.rack_id = 5 THEN 'S4'
                    WHEN s_old.rack_id = 6 AND s_old.name = 'C18' THEN 'S5'
                    WHEN s_old.rack_id IN (7, 8) AND s_old.name = 'C18' THEN 'S6'
                    WHEN s_old.rack_id IN (6, 7, 8) AND s_old.name <> 'C18' THEN 'S4'
                  END
WHERE s_old.id BETWEEN 6 AND 93
  AND s_old.name ~ '^C[0-9]+$';

-- 4a. Update books on legacy shelves id = 6..93 to their true (Rack C01..C18, Shelf S1..S6)
UPDATE library_books b
SET shelf_id = m.new_shelf_id
FROM _legacy_shelf_mapping m
WHERE b.shelf_id = m.old_shelf_id;

-- 4b. Unassign books on legacy catch-all shelf id = 94 ('Unassigned')
--     (Their original raw shelf_location text is already preserved in library_books.remarks)
UPDATE library_books b
SET shelf_id = NULL
FROM library_shelves s_old
WHERE b.shelf_id = s_old.id
  AND s_old.id = 94
  AND s_old.name = 'Unassigned';

-- =============================================================================
-- STEP 5: Delete Obsolete Legacy Shelves (id = 6..94: 'C1'..'C18' & 'Unassigned')
--         Guarded by NOT EXISTS on library_books and library_book_shelf_transfers
-- =============================================================================
\echo '=== STEP 5: Deleting obsolete legacy shelves (id = 6..94) ==='

DELETE FROM library_shelves s
WHERE s.id BETWEEN 6 AND 94
  AND s.name NOT IN ('S1', 'S2', 'S3', 'S4', 'S5', 'S6')
  AND NOT EXISTS (SELECT 1 FROM library_books b WHERE b.shelf_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM library_book_shelf_transfers t WHERE t.old_shelf_id = s.id OR t.new_shelf_id = s.id);

-- =============================================================================
-- STEP 6: Automated In-Transaction Validation Assertions
--         Aborts and rolls back if any count or constraint does not match Excel!
-- =============================================================================
\echo '=== STEP 6: Running automated validation assertions ==='

DO $$
DECLARE
    v_rack_count            INT;
    v_inactive_rack_count   INT;
    v_shelf_count           INT;
    v_inactive_shelf_count  INT;
    v_invalid_shelf_count   INT;
    v_mismatched_rack_count INT;
    v_total_books_before    INT;
    v_assigned_before       INT;
    v_unassigned_94_before  INT;
    v_total_books_after     INT;
    v_assigned_after        INT;
BEGIN
    SELECT COUNT(*), COUNT(*) FILTER (WHERE NOT is_active)
      INTO v_rack_count, v_inactive_rack_count
      FROM library_racks;

    IF v_rack_count <> 25 THEN
        RAISE EXCEPTION 'Validation failed: expected 25 rows in library_racks, found %', v_rack_count;
    END IF;

    IF v_inactive_rack_count <> 0 THEN
        RAISE EXCEPTION 'Validation failed: expected 0 inactive racks, found %', v_inactive_rack_count;
    END IF;

    SELECT COUNT(*),
           COUNT(*) FILTER (WHERE NOT is_active),
           COUNT(*) FILTER (WHERE name NOT IN ('S1','S2','S3','S4','S5','S6') OR code NOT IN ('1','2','3','4','5','6'))
      INTO v_shelf_count, v_inactive_shelf_count, v_invalid_shelf_count
      FROM library_shelves;

    IF v_shelf_count <> 104 THEN
        RAISE EXCEPTION 'Validation failed: expected 104 rows in library_shelves, found %', v_shelf_count;
    END IF;

    IF v_inactive_shelf_count <> 0 THEN
        RAISE EXCEPTION 'Validation failed: expected 0 inactive shelves, found %', v_inactive_shelf_count;
    END IF;

    IF v_invalid_shelf_count <> 0 THEN
        RAISE EXCEPTION 'Validation failed: found % shelves with invalid name/code', v_invalid_shelf_count;
    END IF;

    -- Verify every rack has the exact shelf count and code specified in _target_racks
    SELECT COUNT(*)
      INTO v_mismatched_rack_count
      FROM _target_racks tr
      LEFT JOIN library_racks r ON r.name = tr.rack_name
      LEFT JOIN (
          SELECT rack_id, COUNT(*) AS cnt
          FROM library_shelves
          GROUP BY rack_id
      ) sc ON sc.rack_id = r.id
     WHERE r.id IS NULL
        OR r.code <> tr.rack_code
        OR COALESCE(sc.cnt, 0) <> tr.shelf_count;

    IF v_mismatched_rack_count <> 0 THEN
        RAISE EXCEPTION 'Validation failed: % racks did not match target code or shelf count', v_mismatched_rack_count;
    END IF;

    -- Verify book counts
    SELECT total_books_before, assigned_books_before, unassigned_shelf_books_before
      INTO v_total_books_before, v_assigned_before, v_unassigned_94_before
      FROM _baseline_counts;

    SELECT COUNT(*), COUNT(*) FILTER (WHERE shelf_id IS NOT NULL)
      INTO v_total_books_after, v_assigned_after
      FROM library_books;

    IF v_total_books_after <> v_total_books_before THEN
        RAISE EXCEPTION 'Validation failed: total library_books changed from % to %', v_total_books_before, v_total_books_after;
    END IF;

    IF v_assigned_after <> (v_assigned_before - v_unassigned_94_before) THEN
        RAISE EXCEPTION 'Validation failed: expected % assigned books after update, found %',
            (v_assigned_before - v_unassigned_94_before), v_assigned_after;
    END IF;

    RAISE NOTICE 'ALL VALIDATION ASSERTIONS PASSED: 25 Racks, 104 Shelves, % Assigned Books, % Total Books.',
        v_assigned_after, v_total_books_after;
END $$;

COMMIT;

-- =============================================================================
-- STEP 7: Post-Commit Validation Queries (Read-Only Summary)
-- =============================================================================
\echo '=== POST-COMMIT VALIDATION 1: All 25 Racks with Shelf & Book Counts ==='
SELECT
    r.id AS rack_db_id,
    r.name AS rack_name,
    r.code AS rack_code,
    r.is_active AS rack_active,
    COUNT(s.id) AS shelf_count,
    string_agg(s.name || '(' || s.code || ')', ', ' ORDER BY s.name) AS shelves,
    COALESCE(SUM(bc.book_cnt), 0) AS total_books
FROM library_racks r
LEFT JOIN library_shelves s ON s.rack_id = r.id
LEFT JOIN (
    SELECT shelf_id, COUNT(*) AS book_cnt
    FROM library_books
    WHERE shelf_id IS NOT NULL
    GROUP BY shelf_id
) bc ON bc.shelf_id = s.id
GROUP BY r.id, r.name, r.code, r.is_active
ORDER BY r.name;

\echo '=== POST-COMMIT VALIDATION 2: Overall Table & Book Summary ==='
SELECT
    (SELECT COUNT(*) FROM library_racks) AS total_racks,
    (SELECT COUNT(*) FROM library_racks WHERE is_active) AS active_racks,
    (SELECT COUNT(*) FROM library_shelves) AS total_shelves,
    (SELECT COUNT(*) FROM library_shelves WHERE is_active) AS active_shelves,
    (SELECT COUNT(*) FROM library_books) AS total_books,
    (SELECT COUNT(*) FROM library_books WHERE shelf_id IS NOT NULL) AS books_with_shelf,
    (SELECT COUNT(*) FROM library_books WHERE shelf_id IS NULL) AS books_without_shelf,
    (SELECT COUNT(*) FROM library_book_shelf_transfers) AS total_shelf_transfers;
