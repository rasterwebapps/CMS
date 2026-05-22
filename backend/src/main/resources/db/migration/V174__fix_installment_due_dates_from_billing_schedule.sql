-- V174: Fix installment_fees due dates that were stored using the current date
-- instead of the academic year's configured billing schedule.
--
-- Priority:
--   1. Use the target academic year's term billing schedule due_date (if that AY + billing exists).
--   2. Fall back: take the admission year's billing due_date and shift it forward by
--      (year_number - 1) full years, preserving the same month and day.

UPDATE installment_fees sf
SET due_date = COALESCE(
    -- 1. Target academic year's billing schedule
    (
        SELECT tbs.due_date
        FROM student_fee_allocations sfa
        JOIN students               s        ON s.id  = sfa.student_id
        JOIN cohorts                c        ON c.id  = s.cohort_id
        JOIN academic_years         ay_admit ON ay_admit.id = c.admission_academic_year_id
        JOIN academic_years         ay_target
            ON ay_target.name LIKE (
                CAST(
                    EXTRACT(YEAR FROM ay_admit.start_date)::INTEGER + sf.year_number - 1
                AS TEXT) || '%'
            )
        JOIN term_billing_schedules tbs
            ON tbs.academic_year_id = ay_target.id
           AND tbs.term_type = CASE WHEN sf.sequence = 1 THEN 'ODD' ELSE 'EVEN' END
        WHERE sfa.id = sf.allocation_id
    ),
    -- 2. Fallback: admission year's billing due_date shifted forward
    (
        SELECT (tbs_admit.due_date + ((sf.year_number - 1) * INTERVAL '1 year'))::DATE
        FROM student_fee_allocations sfa
        JOIN students               s        ON s.id  = sfa.student_id
        JOIN cohorts                c        ON c.id  = s.cohort_id
        JOIN term_billing_schedules tbs_admit
            ON tbs_admit.academic_year_id = c.admission_academic_year_id
           AND tbs_admit.term_type = CASE WHEN sf.sequence = 1 THEN 'ODD' ELSE 'EVEN' END
        WHERE sfa.id = sf.allocation_id
    )
)
WHERE sf.due_date IS NOT NULL;
