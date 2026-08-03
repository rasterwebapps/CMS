-- Portion-completion blueprint: a frozen "planned completion date" per syllabus unit for one
-- course offering, computed once from that offering's real timetable occurrences (see
-- PortionBlueprintService.generateBlueprint). Deliberately per-offering, not per-curriculum, since
-- the timetable (and therefore the pacing) is section/offering-specific even though the unit list
-- itself is shared curriculum-level data (syllabus_units). Regenerating (re-running the same admin
-- action) replaces every row for that offering -- see generateBlueprint's delete-then-insert.

CREATE TABLE syllabus_unit_plan (
    id                        BIGSERIAL PRIMARY KEY,
    course_offering_id        BIGINT NOT NULL REFERENCES course_offerings(id) ON DELETE CASCADE,
    syllabus_unit_id          BIGINT NOT NULL REFERENCES syllabus_units(id) ON DELETE CASCADE,
    planned_completion_date   DATE NOT NULL,
    planned_cumulative_hours  INTEGER NOT NULL,
    sequence_index            INTEGER NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_syllabus_unit_plan_offering_unit UNIQUE (course_offering_id, syllabus_unit_id)
);

CREATE INDEX idx_syllabus_unit_plan_course_offering_id ON syllabus_unit_plan(course_offering_id);
