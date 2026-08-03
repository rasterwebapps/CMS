-- Cohort-level physical location allocation: the one committed Theory classroom for a
-- Cohort+TermInstance, term-scoped (no day/period concept here -- that belongs to the
-- later Staffing pass). Lab/Clinical venue assignments live on batches.cohort_room_allocation_id
-- (see V360) since a cohort can need several lab/clinical batches, but exactly one theory room.
CREATE TABLE cohort_room_allocations (
    id                   BIGSERIAL PRIMARY KEY,
    cohort_id            BIGINT NOT NULL REFERENCES cohorts(id),
    term_instance_id     BIGINT NOT NULL REFERENCES term_instances(id),
    theory_classroom_id  BIGINT NOT NULL REFERENCES classrooms(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'COMMITTED',
    committed_by         VARCHAR(255),
    committed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reverted_by          VARCHAR(255),
    reverted_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cohort_room_allocation_status CHECK (status IN ('COMMITTED', 'REVERTED'))
);

CREATE INDEX idx_cohort_room_allocations_cohort ON cohort_room_allocations(cohort_id);
CREATE INDEX idx_cohort_room_allocations_term ON cohort_room_allocations(term_instance_id);
CREATE INDEX idx_cohort_room_allocations_classroom ON cohort_room_allocations(theory_classroom_id);

-- One active (COMMITTED) allocation per cohort/term.
CREATE UNIQUE INDEX ux_cohort_room_alloc_active
    ON cohort_room_allocations(cohort_id, term_instance_id) WHERE status = 'COMMITTED';

-- Two different cohorts can never claim the same Theory classroom as their home room
-- in the same term -- a classroom is one cohort's standing home room for the whole term.
CREATE UNIQUE INDEX ux_theory_classroom_per_term
    ON cohort_room_allocations(theory_classroom_id, term_instance_id) WHERE status = 'COMMITTED';