-- V161 — Add management_seats and counselling_seats to cohorts table.
--
-- Seat allocation is per cohort (program × academic year), not per program,
-- because the government may revise approved intake for a program from year to year.
-- This preserves the historical record: each cohort carries its own approved intake.
--
-- management_seats  — seats the college fills independently
-- counselling_seats — seats filled through government counselling process
--
-- Nullable: existing cohorts will have NULL until data is entered.

ALTER TABLE cohorts
    ADD COLUMN IF NOT EXISTS management_seats INTEGER;

ALTER TABLE cohorts
    ADD COLUMN IF NOT EXISTS counselling_seats INTEGER;
