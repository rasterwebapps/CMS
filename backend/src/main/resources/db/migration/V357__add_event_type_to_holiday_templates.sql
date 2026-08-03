-- Holiday Templates originally only ever seeded HOLIDAY-type calendar events (hence the name).
-- Extended so a repeating event created inline from the Add Event form's "Repeats" picker can be
-- any event type (Exam, Cultural, Sports, Workshop, Other), not just Holiday -- the template now
-- records which eventType it seeds. Defaults to HOLIDAY for every template created so far, since
-- that's exactly what they were.
ALTER TABLE holiday_templates
    ADD COLUMN event_type VARCHAR(20) NOT NULL DEFAULT 'HOLIDAY';
