-- V254: Replace library_books.shelf_location (free text) with a proper shelf_id FK
-- (Library -> Rack -> Shelf tier). Parsed against the real conventions found in the
-- production import data (2666 books):
--   "C{n}-R{n}" / "C{n} R{n}" (shelf number may be Roman numeral, e.g. "C XIII R3")
--       -> Rack "R{n}", Shelf "C{n}"  (dominant convention, ~2245 books)
--   "E{n}" alone (digit or Roman, e.g. "EXII", "E17")
--       -> Rack "E", Shelf "{n}"      (a real separate section, ~5 books)
--   Anything else (shelf number with no rack e.g. "C18"; typos e.g. "C11 P3"; bare "XV")
--       -> single shared Rack "Unassigned", Shelf "Unassigned" — staff re-files these
--          individually later via the Rack/Shelf master screens once physically checked.
--          The original raw shelf_location text is preserved in each such book's remarks
--          field (step 8 below) before the column is dropped, so the clue isn't lost.

-- 1. Add new FK columns (nullable until backfilled)
ALTER TABLE library_books ADD COLUMN library_id BIGINT REFERENCES libraries(id);
ALTER TABLE library_books ADD COLUMN shelf_id   BIGINT REFERENCES library_shelves(id);

-- 2. Roman numeral lookup (I..XX covers every value observed in the real data)
CREATE TEMP TABLE tmp_roman(roman TEXT PRIMARY KEY, arabic TEXT);
INSERT INTO tmp_roman VALUES
    ('I','1'),('II','2'),('III','3'),('IV','4'),('V','5'),('VI','6'),('VII','7'),('VIII','8'),('IX','9'),('X','10'),
    ('XI','11'),('XII','12'),('XIII','13'),('XIV','14'),('XV','15'),('XVI','16'),('XVII','17'),('XVIII','18'),('XIX','19'),('XX','20');

-- 3. Resolve each distinct existing shelf_location value into a (rack_name, shelf_name) pair.
CREATE TEMP TABLE tmp_shelf_resolution AS
WITH normalized AS (
    SELECT DISTINCT shelf_location AS loc,
           regexp_replace(upper(btrim(shelf_location)), '\s+', ' ', 'g') AS norm
    FROM library_books
    WHERE shelf_location IS NOT NULL AND btrim(shelf_location) <> ''
),
cr AS (
    SELECT loc,
           (regexp_match(norm, '^C\s*([IVXLCDM]+|\d+)\s*-?\s*R\s*(\d+)$'))[1] AS shelf_part,
           (regexp_match(norm, '^C\s*([IVXLCDM]+|\d+)\s*-?\s*R\s*(\d+)$'))[2] AS rack_part
    FROM normalized
),
e AS (
    SELECT loc,
           (regexp_match(norm, '^E\s*([IVXLCDM]+|\d+)$'))[1] AS shelf_part
    FROM normalized
)
SELECT
    n.loc,
    CASE
        WHEN cr.rack_part IS NOT NULL THEN 'R' || cr.rack_part
        WHEN e.shelf_part IS NOT NULL THEN 'E'
        ELSE 'Unassigned'
    END AS rack_name,
    CASE
        WHEN cr.rack_part IS NOT NULL THEN 'C' || COALESCE((SELECT arabic FROM tmp_roman WHERE roman = cr.shelf_part), cr.shelf_part)
        WHEN e.shelf_part  IS NOT NULL THEN COALESCE((SELECT arabic FROM tmp_roman WHERE roman = e.shelf_part), e.shelf_part)
        ELSE 'Unassigned'
    END AS shelf_name
FROM normalized n
LEFT JOIN cr ON cr.loc = n.loc
LEFT JOIN e  ON e.loc  = n.loc;

DROP TABLE tmp_roman;

-- 4. Create one rack per distinct rack_name, scoped to Main Library.
WITH racks_coded AS (
    SELECT DISTINCT rack_name, regexp_replace(rack_name, '[^A-Za-z0-9]+', '_', 'g') AS base_code
    FROM tmp_shelf_resolution
),
racks_deduped AS (
    SELECT rack_name, base_code,
           ROW_NUMBER() OVER (PARTITION BY base_code ORDER BY rack_name) AS rn
    FROM racks_coded
)
INSERT INTO library_racks (library_id, name, code)
SELECT
    (SELECT id FROM libraries WHERE code = 'MAIN'),
    rack_name,
    CASE WHEN rn = 1 THEN upper(base_code) ELSE upper(base_code) || '_' || rn END
FROM racks_deduped;

-- 5. Create one shelf tier per distinct (rack_name, shelf_name) pair under its rack.
WITH shelves_coded AS (
    SELECT DISTINCT rack_name, shelf_name, regexp_replace(shelf_name, '[^A-Za-z0-9]+', '_', 'g') AS base_code
    FROM tmp_shelf_resolution
),
shelves_deduped AS (
    SELECT rack_name, shelf_name, base_code,
           ROW_NUMBER() OVER (PARTITION BY rack_name, base_code ORDER BY shelf_name) AS rn
    FROM shelves_coded
)
INSERT INTO library_shelves (rack_id, name, code)
SELECT
    r.id,
    sd.shelf_name,
    CASE WHEN sd.rn = 1 THEN upper(sd.base_code) ELSE upper(sd.base_code) || '_' || sd.rn END
FROM shelves_deduped sd
JOIN library_racks r ON r.name = sd.rack_name AND r.library_id = (SELECT id FROM libraries WHERE code = 'MAIN');

-- 6. Backfill shelf_id/library_id for books that had a shelf_location
UPDATE library_books b
   SET shelf_id   = sh.id,
       library_id = r.library_id
  FROM tmp_shelf_resolution t
  JOIN library_racks r  ON r.name = t.rack_name AND r.library_id = (SELECT id FROM libraries WHERE code = 'MAIN')
  JOIN library_shelves sh ON sh.rack_id = r.id AND sh.name = t.shelf_name
 WHERE btrim(b.shelf_location) = t.loc;

DROP TABLE tmp_shelf_resolution;

-- 7. Every book belongs to the one physical library even if its shelf is unassigned
UPDATE library_books
   SET library_id = (SELECT id FROM libraries WHERE code = 'MAIN')
 WHERE library_id IS NULL;

ALTER TABLE library_books ALTER COLUMN library_id SET NOT NULL;

-- 8. Books that landed in the "Unassigned" catch-all had a shelf_location that didn't match
--    any known convention. Preserve that original text in remarks before it's lost below,
--    so staff re-filing these into a real rack/shelf have the original clue to go on.
UPDATE library_books b
   SET remarks = CASE
                    WHEN b.remarks IS NOT NULL AND btrim(b.remarks) <> ''
                      THEN b.remarks || ' | Original shelf location: ' || b.shelf_location
                    ELSE 'Original shelf location: ' || b.shelf_location
                  END
  FROM library_shelves sh
  JOIN library_racks r ON sh.rack_id = r.id
 WHERE b.shelf_id = sh.id
   AND r.name = 'Unassigned'
   AND b.shelf_location IS NOT NULL;

-- 9. Drop the old free-text column and its index
DROP INDEX IF EXISTS idx_library_books_shelf_location;
ALTER TABLE library_books DROP COLUMN shelf_location;

-- 10. Index the new FK columns
CREATE INDEX idx_library_books_library ON library_books (library_id);
CREATE INDEX idx_library_books_shelf   ON library_books (shelf_id);
