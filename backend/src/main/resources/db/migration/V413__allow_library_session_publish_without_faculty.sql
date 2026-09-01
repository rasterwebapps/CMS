-- LIBRARY gap-fill sessions (TimetableGlobalAutoScheduleService#fillLibraryGaps) are deliberately
-- never staffed -- no faculty is assigned, ever, by design (see that method's javadoc). The
-- existing chk_class_schedule_session_shape (V335) requires faculty_id NOT NULL for ANY PUBLISHED
-- row, with no case for session_type='LIBRARY' at all -- so a Library row could never be published,
-- permanently stuck in DRAFT. Restructured so LIBRARY only needs a classroom once PUBLISHED, while
-- every other session type keeps its exact pre-existing faculty+room requirement unchanged.
ALTER TABLE class_schedules DROP CONSTRAINT chk_class_schedule_session_shape;
ALTER TABLE class_schedules ADD CONSTRAINT chk_class_schedule_session_shape CHECK (
  period_id IS NOT NULL
  AND (
    status <> 'PUBLISHED'
    OR (session_type = 'LIBRARY' AND classroom_id IS NOT NULL)
    OR (
      faculty_id IS NOT NULL
      AND (
        (session_type = 'LAB'      AND lab_id IS NOT NULL) OR
        (session_type = 'THEORY'   AND classroom_id IS NOT NULL) OR
        (session_type = 'CLINICAL' AND clinical_venue_id IS NOT NULL)
      )
    )
  )
);
