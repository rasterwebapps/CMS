-- OC-260: Timetable Draft Review is being retired into Skeleton Builder, with Approve/Publish
-- becoming cohort-scoped instead of whole-term. term_instances.conflict_acknowledged_at /
-- conflict_acknowledged_cell_count (see V528) has no cohort axis at all, so a cohort's own
-- "Conflicts Resolved" stage needs its own per-cohort acknowledgment row instead.
-- Column names/types verified against term_instances' own conflict_acknowledged_at (TIMESTAMPTZ)
-- and conflict_acknowledged_cell_count (INTEGER) columns (V528__timetable_conflict_acknowledgment_gate.sql).

CREATE TABLE cohort_conflict_acknowledgments (
    id                      BIGSERIAL PRIMARY KEY,
    term_instance_id        BIGINT NOT NULL REFERENCES term_instances(id) ON DELETE CASCADE,
    cohort_id               BIGINT NOT NULL REFERENCES cohorts(id) ON DELETE CASCADE,
    acknowledged_at         TIMESTAMPTZ NOT NULL,
    acknowledged_cell_count INTEGER NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cohort_conflict_ack_term_cohort UNIQUE (term_instance_id, cohort_id)
);
