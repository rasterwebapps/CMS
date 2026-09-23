-- Backfills batches.clinical_shift_group_id for active CLINICAL batches whose link was lost to the
-- Batch/CohortSection recommit drift bug (CohortRoomAllocationService#inheritClinicalShiftGroupIfUnambiguous,
-- added in commit 5cd2006a). That fix only auto-relinks a batch the next time its offering's Capacity
-- Auto-Plan is committed -- it does not retroactively repair batches that already existed when the fix
-- shipped, which is why clinical sessions were still missing from published timetables afterward.
--
-- This applies the exact same "link only when exactly one active, section-compatible shift group
-- exists" rule directly to already-drifted rows. Batches whose offering has more than one active
-- candidate shift group are deliberately left NULL for manual resolution via the Manage Clinical
-- Shift Groups screen, matching application behavior -- the migration must not guess.
--
-- Idempotent: the WHERE guard only ever touches batches still missing the link (clinical_shift_group_id
-- IS NULL), so re-running after the application has since relinked a batch, or after a prior run of
-- this same migration, is a no-op.

WITH candidate_counts AS (
    SELECT
        b.id AS batch_id,
        (SELECT MAX(g.id)
         FROM clinical_shift_groups g
         WHERE g.course_offering_id = b.course_offering_id
           AND g.is_active = TRUE
           AND (g.cohort_section_id IS NULL OR g.cohort_section_id = b.cohort_section_id)
        ) AS sole_candidate_id,
        (SELECT COUNT(*)
         FROM clinical_shift_groups g
         WHERE g.course_offering_id = b.course_offering_id
           AND g.is_active = TRUE
           AND (g.cohort_section_id IS NULL OR g.cohort_section_id = b.cohort_section_id)
        ) AS candidate_count
    FROM batches b
    WHERE b.is_active = TRUE
      AND b.clinical_venue_id IS NOT NULL
      AND b.clinical_shift_group_id IS NULL
)
UPDATE batches
SET clinical_shift_group_id = candidate_counts.sole_candidate_id,
    updated_at = NOW()
FROM candidate_counts
WHERE batches.id = candidate_counts.batch_id
  AND candidate_counts.candidate_count = 1;
