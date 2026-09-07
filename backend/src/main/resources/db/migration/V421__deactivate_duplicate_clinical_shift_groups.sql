-- Deactivates exact duplicate ClinicalShiftGroup rows (same offering/section/term/day/start-time)
-- left over from earlier manual admin-dialog testing -- keeps the lowest id in each duplicate
-- set, deactivates the rest. Written as a generic dedup (not a hardcoded id) since these
-- duplicates are local-dev-only leftover test data and may not exist, or may have different ids,
-- in every environment -- a no-op wherever none exist. Deactivating rather than deleting matches
-- the existing soft-delete convention (is_active) already used by deactivateGroup().
UPDATE clinical_shift_groups
SET is_active = false, updated_at = now()
WHERE is_active = true
  AND id NOT IN (
      SELECT MIN(id)
      FROM clinical_shift_groups
      WHERE is_active = true
      GROUP BY course_offering_id, COALESCE(cohort_section_id, -1), term_instance_id, day_of_week, clinical_start_time
  );
