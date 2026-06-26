-- V237 backfilled academic_year_id via (a) linked admission joining year and
-- (b) program intake-window rules. Enquiries whose program had no matching
-- active intake window were left with academic_year_id = NULL and became
-- invisible whenever any academic-year filter was selected in the UI.
--
-- This migration runs the same "upcoming year" fallback that
-- EnquiryService.resolveUpcomingAcademicYear() uses for new enquiries:
-- find the academic year whose start_date is the earliest date that is
-- still after the current year's start_date.  Only runs if a current year
-- and at least one future year exist; otherwise it is a safe no-op.

UPDATE enquiries
SET academic_year_id = (
    SELECT upcoming.id
    FROM academic_years upcoming
    CROSS JOIN (
        SELECT start_date AS cutoff
        FROM   academic_years
        WHERE  is_current = true
        ORDER  BY start_date DESC
        LIMIT  1
    ) cur
    WHERE upcoming.start_date > cur.cutoff
    ORDER BY upcoming.start_date ASC
    LIMIT 1
)
WHERE academic_year_id IS NULL
  AND EXISTS (
      SELECT 1 FROM academic_years WHERE is_current = true
  )
  AND EXISTS (
      SELECT 1
      FROM   academic_years upcoming2
      CROSS JOIN (
          SELECT start_date AS cutoff
          FROM   academic_years
          WHERE  is_current = true
          ORDER  BY start_date DESC
          LIMIT  1
      ) cur2
      WHERE upcoming2.start_date > cur2.cutoff
  );
