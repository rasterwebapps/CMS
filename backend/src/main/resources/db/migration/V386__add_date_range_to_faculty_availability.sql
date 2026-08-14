-- V386: Faculty Availability blocks currently recur indefinitely (day+time only, no date scoping
-- at all). Adds an optional date range so a block can instead apply only for a set of weeks
-- (e.g. "every Monday, Aug 1 - Oct 15") rather than forever. NULL/NULL (both columns) preserves
-- today's exact behavior for every existing row and remains the default for new ones.
ALTER TABLE faculty_availability ADD COLUMN start_date DATE;
ALTER TABLE faculty_availability ADD COLUMN end_date DATE;

ALTER TABLE faculty_availability ADD CONSTRAINT chk_faculty_availability_date_range
    CHECK (
        (start_date IS NULL AND end_date IS NULL)
        OR (start_date IS NOT NULL AND end_date IS NOT NULL AND end_date >= start_date)
    );
