-- Links a seeded HOLIDAY calendar_events row back to the holiday_templates row that generated it
-- (see HolidayTemplateSeedingService). NULL for every manually-created event. ON DELETE SET NULL
-- (not CASCADE): deactivating/deleting a template must never retroactively delete already-occurred
-- historical events -- CalendarEventService's series-delete only ever touches future-dated rows
-- itself, and does so explicitly before a template row is removed.

ALTER TABLE calendar_events
    ADD COLUMN source_holiday_template_id BIGINT REFERENCES holiday_templates(id) ON DELETE SET NULL;

CREATE INDEX idx_calendar_events_source_holiday_template_id ON calendar_events(source_holiday_template_id);
