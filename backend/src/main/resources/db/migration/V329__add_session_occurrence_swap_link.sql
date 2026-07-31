-- Generalized staff-to-staff session swap (Timetable planner Round 2, Phase 7) -- a single-date,
-- two-way trade of who teaches which PUBLISHED session, distinct from Phase 6's absence-driven
-- one-way substitute. Rides entirely on session_occurrences (V322/V327); this migration only adds
-- self-referencing traceability so each side of a swap can be looked up from the other.

ALTER TABLE session_occurrences
    ADD COLUMN swap_partner_occurrence_id BIGINT REFERENCES session_occurrences(id);
