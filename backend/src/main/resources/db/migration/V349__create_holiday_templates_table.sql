-- Master list of recurring holiday rules (e.g. "Republic Day" every Jan 26, "2nd Saturday off"
-- every month) that get materialized into concrete calendar_events rows for each AcademicYear at
-- creation time (see HolidayTemplateSeedingService). Independent of any single AcademicYear -- a
-- template outlives any one year's calendar, unlike calendar_events which is always scoped to one.
-- WEEKLY recurrence is deliberately NOT supported here -- a standing weekly closure (e.g. every
-- Sunday) is already just a blocked_periods RECURRING rule with no calendar_events row needed.

CREATE TABLE holiday_templates (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL UNIQUE,
    recurrence_type   VARCHAR(20) NOT NULL,
    holiday_category  VARCHAR(20),
    description       TEXT,
    duration_days     INTEGER NOT NULL DEFAULT 1,
    month             INTEGER,
    day_of_month      INTEGER,
    week_of_month     VARCHAR(20),
    day_of_week       VARCHAR(20),
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_holiday_template_duration CHECK (duration_days > 0),
    CONSTRAINT chk_holiday_template_shape CHECK (
        (recurrence_type = 'YEARLY'
            AND month BETWEEN 1 AND 12 AND day_of_month BETWEEN 1 AND 31
            AND week_of_month IS NULL AND day_of_week IS NULL)
        OR
        (recurrence_type = 'MONTHLY'
            AND month IS NULL AND day_of_month IS NULL
            AND week_of_month IS NOT NULL AND day_of_week IS NOT NULL)
    )
);

CREATE INDEX idx_holiday_templates_is_active ON holiday_templates(is_active);
