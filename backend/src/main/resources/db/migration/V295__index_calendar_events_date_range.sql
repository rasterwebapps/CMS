-- Supports the new date-range holiday query (CalendarEventRepository.findOverlapping) used by
-- the personal/browse timetable views to annotate a specific displayed week. No schema change to
-- calendar_events itself -- it already has start_date/end_date/event_type/academic_year_id.

CREATE INDEX idx_calendar_events_date_range ON calendar_events(academic_year_id, event_type, start_date, end_date);
