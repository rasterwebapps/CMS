-- Adds no column: curriculum_term_courses.subject_type (V266) is VARCHAR(20) with no CHECK
-- constraint, so the new CO_CURRICULAR SubjectType value needs no schema change here.
--
-- Backfill: any curriculum subject entered as CORE/FOUNDATIONAL/ELECTIVE whose name matches the
-- self-study/co-curricular naming pattern the timetable auto-scheduler already recognizes (see
-- com.cms.util.SelfStudySubjects) is reclassified as CO_CURRICULAR, across every curriculum
-- version for every client — this is a systemic data-entry gap (the schema had no way to mark a
-- subject as advisory before this migration), not a single cohort's mistake.
UPDATE curriculum_term_courses
SET subject_type = 'CO_CURRICULAR'
WHERE subject_type <> 'CO_CURRICULAR'
  AND subject_id IN (
    SELECT id FROM subjects
    WHERE LOWER(name) LIKE '%self-study%'
       OR LOWER(name) LIKE '%self study%'
       OR LOWER(name) LIKE '%co-curricular%'
       OR LOWER(name) LIKE '%cocurricular%'
  );
