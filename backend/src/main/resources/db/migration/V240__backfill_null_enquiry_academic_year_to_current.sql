-- V239 used "find year starting after current year's start_date" as the
-- fallback, which is a no-op when the latest year IS the current year
-- (there is no future year to find). In that state, EnquiryService
-- .resolveUpcomingAcademicYear() falls back to .orElse(current), so new
-- enquiries already get the current year. Apply the same rule here to
-- close the remaining 46 NULL rows left by V237/V239.

UPDATE enquiries
SET academic_year_id = (
    SELECT id FROM academic_years WHERE is_current = true LIMIT 1
)
WHERE academic_year_id IS NULL
  AND EXISTS (SELECT 1 FROM academic_years WHERE is_current = true);
