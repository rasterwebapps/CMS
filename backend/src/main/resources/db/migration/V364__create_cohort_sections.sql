-- A CohortRoomAllocation's Theory room commitment, split into one or more sections when a
-- cohort is too big for any single classroom. Every allocation has at least one section, even
-- the common unsectioned case (exactly one section = the room the old single theory_classroom_id
-- column used to hold directly, see V366 which removes that column once this table is populated).
CREATE TABLE cohort_sections (
    id                          BIGSERIAL PRIMARY KEY,
    cohort_room_allocation_id  BIGINT NOT NULL REFERENCES cohort_room_allocations(id),
    term_instance_id            BIGINT NOT NULL REFERENCES term_instances(id),
    section_label                VARCHAR(100) NOT NULL,
    classroom_id                 BIGINT NOT NULL REFERENCES classrooms(id),
    planned_size                  INTEGER NOT NULL,
    is_active                    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cohort_section_planned_size_positive CHECK (planned_size > 0)
);

CREATE INDEX idx_cohort_sections_allocation ON cohort_sections(cohort_room_allocation_id);
CREATE INDEX idx_cohort_sections_classroom ON cohort_sections(classroom_id);

-- No two active sections -- even across different cohorts -- can share a classroom in the same
-- term, carrying forward the guarantee the old ux_theory_classroom_per_term gave at the header
-- level (see V366).
CREATE UNIQUE INDEX ux_cohort_section_classroom_per_term
    ON cohort_sections(classroom_id, term_instance_id) WHERE is_active;

-- Section labels are unique within one allocation ("Section 1", "Section 2", ...).
CREATE UNIQUE INDEX ux_cohort_section_label_per_allocation
    ON cohort_sections(cohort_room_allocation_id, section_label);

-- Backfill: one section per existing allocation, carrying its theory_classroom_id forward.
-- planned_size is backfilled from the room's own capacity -- the real originally-committed
-- headcount was never stored, so the room's capacity is the safest non-guessed upper bound.
INSERT INTO cohort_sections (cohort_room_allocation_id, term_instance_id, section_label, classroom_id, planned_size, is_active)
SELECT cra.id, cra.term_instance_id, 'Section 1', cra.theory_classroom_id,
       COALESCE(c.capacity, 1), (cra.status = 'COMMITTED')
FROM cohort_room_allocations cra
JOIN classrooms c ON c.id = cra.theory_classroom_id;
