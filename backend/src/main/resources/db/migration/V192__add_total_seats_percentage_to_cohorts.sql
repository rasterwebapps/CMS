-- V192 — Add total_seats and management_percentage to cohorts.
--
-- Seat allocation is now driven by total intake + management quota percentage.
-- management_seats and counselling_seats are derived from these two values and
-- stored for fast reads (avoid recomputing on every query).
--
-- management_percentage: stored as decimal, e.g. 35.00 represents 35%
-- management_seats = round(total_seats * management_percentage / 100)
-- counselling_seats = total_seats - management_seats

ALTER TABLE cohorts
    ADD COLUMN IF NOT EXISTS total_seats          INTEGER,
    ADD COLUMN IF NOT EXISTS management_percentage NUMERIC(5, 2);
