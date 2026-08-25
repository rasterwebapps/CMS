-- Retired: replaced by the eligible-faculty picker (Speciality match OR the subject's Eligible
-- Faculty list, annotated with real remaining term capacity) used for both the initial Assign
-- Faculty pick and later reassignment, and by Section Faculty's per-section override now that it's
-- authoritative for placement. Nothing else reads this column -- verified against Java code
-- (CourseOffering, CourseOfferingServiceImpl, CourseOfferingDto/UpdateRequest,
-- TimetableGlobalAutoScheduleService, SpreadLoadSuggestion) before writing this DROP, per the
-- migration column-verification gate.
ALTER TABLE course_offerings DROP COLUMN secondary_faculty_id;
