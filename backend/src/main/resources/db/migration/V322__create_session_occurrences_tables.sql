-- Session-linked portion-completion progress tracking (Phase 3 of the Timetable planner Round 2
-- initiative). A ClassSchedule row is a weekly-recurring template with no calendar date (see
-- TimetableGenerationService's class javadoc); session_occurrences is the "this recurring row
-- actually happened on this specific date" anchor -- one shared spine table, since Phase 6
-- (faculty-absence substitution, not built yet) needs the identical anchor and will extend this
-- table additively rather than duplicating it. Rows are created lazily, only when a faculty
-- member actually logs something for a date -- never pre-populated for every possible occurrence.

CREATE TABLE session_occurrences (
    id                      BIGSERIAL PRIMARY KEY,
    class_schedule_id       BIGINT NOT NULL REFERENCES class_schedules(id),
    occurrence_date         DATE NOT NULL,
    recorded_by_faculty_id  BIGINT REFERENCES faculty(id),
    remarks                 VARCHAR(1000),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_session_occurrences_schedule_date UNIQUE (class_schedule_id, occurrence_date)
);

CREATE INDEX idx_session_occurrences_class_schedule ON session_occurrences(class_schedule_id);

-- Many units can be covered in one occurrence; the same unit can span several occurrences.
CREATE TABLE session_occurrence_units (
    session_occurrence_id  BIGINT NOT NULL REFERENCES session_occurrences(id) ON DELETE CASCADE,
    syllabus_unit_id       BIGINT NOT NULL REFERENCES syllabus_units(id),
    PRIMARY KEY (session_occurrence_id, syllabus_unit_id)
);

CREATE INDEX idx_session_occurrence_units_unit ON session_occurrence_units(syllabus_unit_id);
