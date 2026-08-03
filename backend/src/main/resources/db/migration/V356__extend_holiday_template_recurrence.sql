-- Extends holiday_templates to support the full "repeats?" flow requested directly from the
-- Add Event form (mirroring the iOS/Google Calendar Repeat picker): DAILY and WEEKLY join the
-- existing YEARLY/MONTHLY recurrence types, every type gets an interval ("every N units"), an
-- optional anchor_date (the first/reference occurrence -- required whenever interval > 1, or for
-- DAILY/WEEKLY generally, so "every 2 weeks" has something to count from), and an optional
-- end_date ("repeats until"; a UI-side "after N occurrences" choice is translated into this same
-- column at save time rather than stored as a separate count).
--
-- The original chk_holiday_template_shape CHECK (a simple YEARLY-vs-MONTHLY 2-way branch) cannot
-- cleanly express the new shape: 4 recurrence types, plus MONTHLY now supporting TWO mutually
-- exclusive sub-patterns (fixed day-of-month, or nth-weekday-of-month). Rather than write an
-- unwieldy multi-way SQL CHECK, validation moves entirely to HolidayTemplateService.validateShape,
-- which every create/update path already runs through -- dropped here, not replaced.

ALTER TABLE holiday_templates DROP CONSTRAINT IF EXISTS chk_holiday_template_shape;

ALTER TABLE holiday_templates
    ADD COLUMN interval_count INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN anchor_date    DATE,
    ADD COLUMN end_date       DATE;

ALTER TABLE holiday_templates
    ADD CONSTRAINT chk_holiday_template_interval CHECK (interval_count > 0);
