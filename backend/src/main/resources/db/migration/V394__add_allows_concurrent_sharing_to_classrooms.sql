-- Marks a Classroom (e.g. a large lecture/drawing hall) as eligible for concurrent, capacity-pooled
-- sharing in the Special Class Scheduler (BR-55 follow-on): multiple special-class bookings can
-- occupy the same room at the same period as long as their combined registered strength fits the
-- room's capacity, instead of the default one-booking-per-period exclusivity every other classroom
-- still enforces. See SpecialClassRequestService.checkConflicts.

ALTER TABLE classrooms ADD COLUMN allows_concurrent_sharing BOOLEAN NOT NULL DEFAULT FALSE;
