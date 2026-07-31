-- A unit's coverage in one session is not boolean "touched = done": a 12-hour unit spans many
-- periods, a session can finish one unit and start the next, and a unit can genuinely finish in
-- fewer hours than planned (the remaining period time isn't automatically "more unit progress").
-- session_occurrence_units becomes a real record of hours actually spent per unit per session,
-- plus an explicit "marked complete" flag the faculty sets deliberately -- never inferred from
-- hours-logged-vs-planned, since that would misfire whenever a unit finishes early or late.
-- Table has zero rows in every environment this has shipped to (feature is brand new), so it's
-- dropped and recreated rather than altered in place.

DROP TABLE session_occurrence_units;

CREATE TABLE session_occurrence_units (
    id                     BIGSERIAL PRIMARY KEY,
    session_occurrence_id  BIGINT NOT NULL REFERENCES session_occurrences(id) ON DELETE CASCADE,
    syllabus_unit_id       BIGINT NOT NULL REFERENCES syllabus_units(id),
    hours_covered          NUMERIC(5,2),
    marked_complete        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_session_occurrence_units_occurrence_unit UNIQUE (session_occurrence_id, syllabus_unit_id)
);

CREATE INDEX idx_session_occurrence_units_unit ON session_occurrence_units(syllabus_unit_id);
