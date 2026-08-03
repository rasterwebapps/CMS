-- Links a BlockedPeriod row back to the HOLIDAY calendar_events row that auto-generated it (see
-- CalendarEventService.syncHolidayBlocks). NULL for every manually-created block (via the Block
-- Periods mini-form or the full-page Blocked Periods tab) -- this column IS the "auto-generated"
-- discriminator; no separate boolean is introduced. ON DELETE CASCADE: deleting the source event
-- removes its auto-blocks outright. An admin can still delete an individual auto-block row early,
-- independent of the source event, as the "unblock for a special class" override.

ALTER TABLE blocked_periods
    ADD COLUMN source_calendar_event_id BIGINT REFERENCES calendar_events(id) ON DELETE CASCADE;

CREATE INDEX idx_blocked_periods_source_calendar_event_id ON blocked_periods(source_calendar_event_id);
