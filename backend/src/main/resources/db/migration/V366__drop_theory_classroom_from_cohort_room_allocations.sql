-- Theory room(s) now always live on child cohort_sections rows (see V364), including the common
-- unsectioned case (exactly one section) -- so the single theory_classroom_id column on the
-- allocation header is redundant and is dropped here. Also adds an audit trail of what strength
-- number a commit was actually planned against (enrolled headcount vs. university-sanctioned
-- intake), since neither was ever recorded before.
DROP INDEX ux_theory_classroom_per_term;
DROP INDEX idx_cohort_room_allocations_classroom;
ALTER TABLE cohort_room_allocations DROP COLUMN theory_classroom_id;

ALTER TABLE cohort_room_allocations ADD COLUMN planning_basis VARCHAR(20) NOT NULL DEFAULT 'ENROLLED';
ALTER TABLE cohort_room_allocations ADD CONSTRAINT chk_cohort_room_allocation_planning_basis
    CHECK (planning_basis IN ('ENROLLED', 'SANCTIONED'));

ALTER TABLE cohort_room_allocations ADD COLUMN planning_strength INTEGER NOT NULL DEFAULT 0;

-- Backfill planning_strength from each allocation's already-backfilled section sizes.
UPDATE cohort_room_allocations cra
SET planning_strength = sub.total
FROM (
    SELECT cohort_room_allocation_id, SUM(planned_size) AS total
    FROM cohort_sections
    GROUP BY cohort_room_allocation_id
) sub
WHERE sub.cohort_room_allocation_id = cra.id;
