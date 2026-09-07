-- Seeds ClinicalShiftGroup rows so every clinical-bearing course offering actually has a shift
-- window, instead of the near-total gap that existed before this migration (only 3 hand-created
-- rows existed anywhere, covering 1 subject). Days/week per subject are computed from its real
-- curriculum clinical_hours divided by (6h shift x weeks in that term instance), rounded to the
-- nearest whole day -- not rounded up, since rounding up is exactly the over-scheduling bug fixed
-- in TimetableSkeletonService#batchScopedBudgets on 2026-09-03/04 (a small residual forcing a
-- whole extra weekly session). Term_number groups each cohort's clinical subjects onto distinct
-- weekdays so a cohort is never asked to be in two clinical postings on the same day.
--
-- The 5 term_number=8 "Internship (N weeks)" offerings run two full-day shifts (Shift A 7am-1pm,
-- Shift B 1pm-7pm) Mon-Fri, as sequential non-overlapping blocks bounded by the new
-- effective_start_date/effective_end_date columns (V419) -- Community Health(4wk) -> Adult
-- Health(6wk) -> Child Health(4wk) -> Mental Health(4wk) -> Midwifery(4wk), 22 of the term's
-- 26/27 weeks, starting week 1. NOTE: the Skeleton Builder grid still renders each internship's
-- weekday as blocked for the WHOLE term (no per-week grid template exists yet, see V419) -- only
-- hours-crediting and occurrence generation honor the real bounded window.
--
-- Resolved dynamically by (academic year name, term type, subject code) rather than hardcoded
-- ids, so this migration is portable across environments (course_offering ids are auto-generated
-- per-environment and would not otherwise line up) -- per the hard gate to grep exact
-- columns/never guess, a row-count assertion below hard-fails the migration instead of silently
-- seeding fewer rows than intended if a business key here doesn't match an environment's data.
--
-- 138 rows total (38 whole-term regular shift rows across 26 non-internship offerings, 100
-- date-bounded internship shift rows across 5 subjects x 2 EVEN terms x 5 weekdays x 2 shifts).
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    WITH offering_lookup AS (
        SELECT co.id AS course_offering_id, ti.id AS term_instance_id,
               ay.name AS ay_name, ti.term_type, s.code AS subject_code
        FROM course_offerings co
        JOIN term_instances ti ON ti.id = co.term_instance_id
        JOIN academic_years ay ON ay.id = ti.academic_year_id
        JOIN subjects s ON s.id = co.subject_id
    ),
    seed_rows(ay_name, term_type, subject_code, label, day_of_week, clinical_start_time, effective_start_date, effective_end_date) AS (
        VALUES
        ('2026-2027', 'ODD', 'N-NF-I-125', 'N-NF-I-125 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-AHN-I-215', 'N-AHN-I-215 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-AHN-I-215', 'N-AHN-I-215 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-AHN-I-215', 'N-AHN-I-215 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-CHN-I-301', 'N-CHN-I-301 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-COMH-I-310', 'N-COMH-I-310 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-MHN-I-305', 'N-MHN-I-305 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-COMH-II-401', 'N-COMH-II-401 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-MIDW-II-410', 'N-MIDW-II-410 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'ODD', 'N-MIDW-II-410', 'N-MIDW-II-410 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-NF-I-125', 'N-NF-I-125 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-AHN-I-215', 'N-AHN-I-215 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-AHN-I-215', 'N-AHN-I-215 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-AHN-I-215', 'N-AHN-I-215 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-CHN-I-301', 'N-CHN-I-301 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-COMH-I-310', 'N-COMH-I-310 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-MHN-I-305', 'N-MHN-I-305 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-COMH-II-401', 'N-COMH-II-401 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-MIDW-II-410', 'N-MIDW-II-410 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'ODD', 'N-MIDW-II-410', 'N-MIDW-II-410 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-NF-II-125', 'N-NF-II-125 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-NF-II-125', 'N-NF-II-125 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-AHN-II-225', 'N-AHN-II-225 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-AHN-II-225', 'N-AHN-II-225 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-AHN-II-225', 'N-AHN-II-225 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-CHN-II-306', 'N-CHN-II-306 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-MHN-II-307', 'N-MHN-II-307 Clinical Shift (Tuesday)', 'TUESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'N-MIDW-I-335', 'N-MIDW-I-335 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'NMLE330', 'NMLE330 Clinical Shift (Thursday)', 'THURSDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-NF-II-125', 'N-NF-II-125 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-NF-II-125', 'N-NF-II-125 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-AHN-II-225', 'N-AHN-II-225 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-AHN-II-225', 'N-AHN-II-225 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-AHN-II-225', 'N-AHN-II-225 Clinical Shift (Friday)', 'FRIDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-CHN-II-306', 'N-CHN-II-306 Clinical Shift (Monday)', 'MONDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-MHN-II-307', 'N-MHN-II-307 Clinical Shift (Tuesday)', 'TUESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'N-MIDW-I-335', 'N-MIDW-I-335 Clinical Shift (Wednesday)', 'WEDNESDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2025-2026', 'EVEN', 'NMLE330', 'NMLE330 Clinical Shift (Thursday)', 'THURSDAY', TIME '07:00:00', NULL::date, NULL::date),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2027-04-01', DATE '2027-04-28'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2027-04-29', DATE '2027-06-09'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2027-06-10', DATE '2027-07-07'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2027-07-08', DATE '2027-08-04'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2026-2027', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2027-08-05', DATE '2027-09-01'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift A -- INTE415 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE415', 'Shift B -- INTE415 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2026-04-01', DATE '2026-04-28'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift A -- INTE420 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE420', 'Shift B -- INTE420 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2026-04-29', DATE '2026-06-09'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift A -- INTE425 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE425', 'Shift B -- INTE425 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2026-06-10', DATE '2026-07-07'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift A -- INTE430 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE430', 'Shift B -- INTE430 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2026-07-08', DATE '2026-08-04'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Monday)', 'MONDAY', TIME '07:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Monday)', 'MONDAY', TIME '13:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Tuesday)', 'TUESDAY', TIME '07:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Tuesday)', 'TUESDAY', TIME '13:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Wednesday)', 'WEDNESDAY', TIME '07:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Wednesday)', 'WEDNESDAY', TIME '13:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Thursday)', 'THURSDAY', TIME '07:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Thursday)', 'THURSDAY', TIME '13:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift A -- INTE435 (Friday)', 'FRIDAY', TIME '07:00:00', DATE '2026-08-05', DATE '2026-09-01'),
        ('2025-2026', 'EVEN', 'INTE435', 'Shift B -- INTE435 (Friday)', 'FRIDAY', TIME '13:00:00', DATE '2026-08-05', DATE '2026-09-01')
    )
    INSERT INTO clinical_shift_groups
        (course_offering_id, term_instance_id, label, day_of_week, clinical_start_time, effective_start_date, effective_end_date, created_at, updated_at)
    SELECT ol.course_offering_id, ol.term_instance_id, sr.label, sr.day_of_week, sr.clinical_start_time,
           sr.effective_start_date, sr.effective_end_date, now(), now()
    FROM seed_rows sr
    JOIN offering_lookup ol
        ON ol.ay_name = sr.ay_name AND ol.term_type = sr.term_type AND ol.subject_code = sr.subject_code;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 138 THEN
        RAISE EXCEPTION 'Expected to insert 138 clinical_shift_group rows, actually inserted %; a subject code, academic year name, or term type here does not match this environment''s data -- check before re-running', v_count;
    END IF;
END $$;
