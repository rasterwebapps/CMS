-- Lets a HOLIDAY calendar event be classified as government / local / institutional, per the
-- academic calendar requirement. Nullable and only meaningful for eventType = HOLIDAY --
-- CalendarEventService clears it for every other event type.

ALTER TABLE calendar_events ADD COLUMN holiday_category VARCHAR(20);
