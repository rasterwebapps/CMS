-- Declares that a specific calendar date runs a DIFFERENT weekday's timetable than its actual
-- weekday (e.g. "Sat 26 Jul 2026 runs Monday's schedule" -- a compensatory working day making up
-- a mid-week holiday). Narrow, automatic, institution-wide: one row per date, resolved at
-- read-time by ClassScheduleOccurrenceService (never materializes new ClassSchedule/
-- SessionOccurrence rows), same "skip/borrow on read" philosophy as the existing BlockedPeriod
-- holiday-skip logic. Term-scoped since ClassScheduleOccurrenceService itself is term-scoped.
-- UNIQUE(mapped_date) -- one date can only ever mean one thing institution-wide. The mapped date
-- ALWAYS fully suppresses its own actual-weekday sessions (enforced in DayMappingOverrideService,
-- not here -- see that class for why: the test profile runs Flyway-disabled H2, so a DB-level
-- CHECK constraint here would never be exercised by tests).

CREATE TABLE day_mapping_overrides (
    id                    BIGSERIAL PRIMARY KEY,
    term_instance_id      BIGINT NOT NULL REFERENCES term_instances(id),
    mapped_date           DATE NOT NULL,
    borrowed_day_of_week  VARCHAR(20) NOT NULL,
    reason                VARCHAR(255) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_day_mapping_overrides_mapped_date UNIQUE (mapped_date)
);

CREATE INDEX idx_day_mapping_overrides_term_instance_id ON day_mapping_overrides(term_instance_id);
CREATE INDEX idx_day_mapping_overrides_borrowed_day_of_week ON day_mapping_overrides(borrowed_day_of_week);
