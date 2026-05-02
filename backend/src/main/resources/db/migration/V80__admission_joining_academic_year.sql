-- Replace raw integer year columns on admissions with a proper FK to academic_years

ALTER TABLE admissions
    ADD COLUMN joining_academic_year_id BIGINT
        REFERENCES academic_years(id);

-- Backfill: match the joining year to the academic year whose start_date falls in that calendar year
UPDATE admissions a
SET joining_academic_year_id = (
    SELECT ay.id
    FROM academic_years ay
    WHERE EXTRACT(YEAR FROM ay.start_date)::INTEGER = a.academic_year_from
    ORDER BY ay.start_date ASC
    LIMIT 1
)
WHERE a.academic_year_from IS NOT NULL;

-- If any row still has no match (no AY exists for that year), fall back to the current/latest AY
UPDATE admissions
SET joining_academic_year_id = (
    SELECT id FROM academic_years
    ORDER BY is_current DESC, start_date DESC
    LIMIT 1
)
WHERE joining_academic_year_id IS NULL;

ALTER TABLE admissions
    ALTER COLUMN joining_academic_year_id SET NOT NULL;

ALTER TABLE admissions
    DROP COLUMN academic_year_from,
    DROP COLUMN academic_year_to;
