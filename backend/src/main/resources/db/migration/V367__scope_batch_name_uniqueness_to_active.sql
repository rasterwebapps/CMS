-- V271's UNIQUE(course_offering_id, name) never accounted for revert(): reverting a
-- CohortRoomAllocation soft-deactivates its batches (is_active = false) rather than deleting them,
-- so roster history survives -- but the plain unique constraint still blocks any future commit from
-- reusing that same (course_offering_id, name) pair forever, even though the row is now dead
-- history. Replacing it with a partial unique index scoped to is_active = true so only currently
-- live batches must have distinct names -- a reverted allocation's batches no longer reserve their
-- name once inactive.
ALTER TABLE batches DROP CONSTRAINT batches_course_offering_id_name_key;

CREATE UNIQUE INDEX ux_batches_course_offering_name_active
    ON batches (course_offering_id, name)
    WHERE is_active;
