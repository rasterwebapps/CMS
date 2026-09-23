-- Extends the broadcast-style notifications table with an optional single-recipient scope, needed
-- for the new holiday-disruption alert (a ONE_OFF BlockedPeriod that cancels an already-PUBLISHED
-- ClassSchedule notifies just the affected faculty + their HOD/coordinator, not every user who can
-- see the category). NULL keeps today's broadcast-to-category behavior (e.g. academicTermAlerts)
-- completely unchanged.
ALTER TABLE notifications
    ADD COLUMN recipient_faculty_id BIGINT REFERENCES faculty(id);

CREATE INDEX idx_notifications_recipient_unresolved
    ON notifications (recipient_faculty_id) WHERE resolved_at IS NULL;

-- Replaces uq_notifications_active_source: the old index allowed only one active row per
-- (source_type, source_id, category_key) at all, which would block inserting one row per
-- recipient (faculty + HOD) for the same disruption. COALESCE folds every broadcast row
-- (recipient_faculty_id IS NULL) into the same bucket, so the original "one active broadcast
-- alert per source" invariant is preserved exactly; a real recipient id gets its own bucket,
-- giving "one active alert per source per recipient" instead.
DROP INDEX uq_notifications_active_source;

CREATE UNIQUE INDEX uq_notifications_active_source_recipient
    ON notifications (source_type, source_id, category_key, COALESCE(recipient_faculty_id, -1))
    WHERE resolved_at IS NULL;
