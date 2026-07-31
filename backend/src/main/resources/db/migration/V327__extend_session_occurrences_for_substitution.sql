-- Additive extension of session_occurrences (V322) for Phase 6 (faculty absence -> substitute)
-- -- the same shared spine used by Phase 3's progress logging, per the Round 2 plan's decision to
-- consolidate rather than build a second "find-or-create this date's occurrence row" table.
-- effective_faculty_id is null unless a substitution was applied for that date (meaning "the
-- recurring template's own faculty taught it, as normal"). ClassSchedule.faculty is NEVER
-- mutated by this feature -- see FacultyAbsenceService.

ALTER TABLE session_occurrences
    ADD COLUMN effective_faculty_id BIGINT REFERENCES faculty(id),
    ADD COLUMN faculty_absence_id   BIGINT REFERENCES faculty_absences(id),
    ADD COLUMN occurrence_status    VARCHAR(20) NOT NULL DEFAULT 'HELD';
