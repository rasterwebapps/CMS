-- Recurring/one-off period blocks (e.g. "Period 5 on 2026-11-15 for a staff meeting", or
-- "Period 1 every Wednesday, 2026-10-01 to 2027-03-31, for a standing staff meeting"). Distinct
-- from calendar_events (whole-day HOLIDAY/EXAM) -- this is period-granular and institution-wide,
-- not tied to a single AcademicYear/TermInstance, since a recurring block is its own standalone
-- rule that may or may not overlap any given term. ONE_OFF blocks only ever affect Capacity
-- Planner buffer-hours math and calendar display; only RECURRING blocks are enforced as a hard
-- placement conflict in the Skeleton Builder (see TimetableSkeletonService.placeCell).

CREATE TABLE blocked_periods (
    id                BIGSERIAL PRIMARY KEY,
    period_id         BIGINT NOT NULL REFERENCES periods(id),
    block_type        VARCHAR(20) NOT NULL,
    specific_date     DATE,
    day_of_week       VARCHAR(20),
    range_start_date  DATE,
    range_end_date    DATE,
    reason            VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blocked_period_shape CHECK (
        (block_type = 'ONE_OFF'
            AND specific_date IS NOT NULL
            AND day_of_week IS NULL AND range_start_date IS NULL AND range_end_date IS NULL)
        OR
        (block_type = 'RECURRING'
            AND specific_date IS NULL
            AND day_of_week IS NOT NULL AND range_start_date IS NOT NULL AND range_end_date IS NOT NULL
            AND range_end_date >= range_start_date)
    )
);

CREATE INDEX idx_blocked_periods_period_id ON blocked_periods(period_id);
CREATE INDEX idx_blocked_periods_day_of_week ON blocked_periods(day_of_week);