-- Incident (2026-07-22): EnquiryService.update() blindly overwrote Program/Course/State/
-- StudentType/AdmissionQuota with null whenever those form controls were disabled (fee-locked
-- edit form omits disabled controls from its submitted value), and separately reset
-- year_wise_fees/final_calculated_fee to null via applyAuthoritativeFees() on every edit of an
-- already fee-finalized enquiry. Both are fixed going forward in EnquiryService.update().
--
-- This backfills the 9 live enquiries corrupted by that bug before the fix was deployed. Program
-- and Course are unambiguous: the system has exactly one of each (id 1). AdmissionQuota and
-- StudentType were reverse-engineered from each enquiry's still-intact gender/fee_state_id/
-- academic_year_id/finalized_total_fee against the current fee_structure_groups totals (with vs.
-- without the HOSTEL_FEE line item distinguishes DAY_SCHOLAR from HOSTELER) — every one of the 9
-- matched exactly one (quota, student_type) combination, with no ties. finalized_net_fee (the
-- actual discounted amount already collected against) is left untouched; year_wise_fees is
-- regenerated as an equal 4-year split of that same net fee, matching the frontend's own
-- applyEqualSplitFallback() logic.
--
-- WHERE program_id IS NULL guards make this idempotent — a re-run is a no-op.

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":250000},{"yearNumber":2,"amount":250000},{"yearNumber":3,"amount":250000},{"yearNumber":4,"amount":250000}]',
    updated_at = now()
WHERE id = 276 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":227500},{"yearNumber":2,"amount":227500},{"yearNumber":3,"amount":227500},{"yearNumber":4,"amount":227500}]',
    updated_at = now()
WHERE id = 277 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":225000},{"yearNumber":2,"amount":225000},{"yearNumber":3,"amount":225000},{"yearNumber":4,"amount":225000}]',
    updated_at = now()
WHERE id = 287 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":250000},{"yearNumber":2,"amount":250000},{"yearNumber":3,"amount":250000},{"yearNumber":4,"amount":250000}]',
    updated_at = now()
WHERE id = 290 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":225000},{"yearNumber":2,"amount":225000},{"yearNumber":3,"amount":225000},{"yearNumber":4,"amount":225000}]',
    updated_at = now()
WHERE id = 292 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'DAY_SCHOLAR',
    year_wise_fees = '[{"yearNumber":1,"amount":220000},{"yearNumber":2,"amount":220000},{"yearNumber":3,"amount":220000},{"yearNumber":4,"amount":220000}]',
    updated_at = now()
WHERE id = 294 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":250000},{"yearNumber":2,"amount":250000},{"yearNumber":3,"amount":250000},{"yearNumber":4,"amount":250000}]',
    updated_at = now()
WHERE id = 295 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'DAY_SCHOLAR',
    year_wise_fees = '[{"yearNumber":1,"amount":220000},{"yearNumber":2,"amount":220000},{"yearNumber":3,"amount":220000},{"yearNumber":4,"amount":220000}]',
    updated_at = now()
WHERE id = 296 AND program_id IS NULL;

UPDATE enquiries SET program_id = 1, course_id = 1, admission_quota = 'MANAGEMENT',
    student_type = 'HOSTELER',
    year_wise_fees = '[{"yearNumber":1,"amount":225000},{"yearNumber":2,"amount":225000},{"yearNumber":3,"amount":225000},{"yearNumber":4,"amount":225000}]',
    updated_at = now()
WHERE id = 297 AND program_id IS NULL;
