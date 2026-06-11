-- ============================================================
-- DATA CORRECTION: Delete 3 students and all related records
-- Students: Ananya Anilkumar, Eldhose Binoy, Arjun B S
-- Date: 2026-06-09
-- Prepared by: Claude Code
-- ============================================================
-- PRE-CONDITION: pg_dump backup of the production DB must be
-- confirmed in place before running any DELETE below.
-- ============================================================


-- ===========================================================
-- STEP 1 — VERIFY (run this block first, confirm the names)
-- IDs confirmed on 2026-06-09 from production:
--   15 — ANANYA   ANILKUMAR  (roll 959652025011)
--   17 — ELDHOSE  BINOY      (roll 959652025020)
--   30 — ARJUN    B S        (roll 959652025008)
-- ===========================================================
SELECT id, first_name, last_name, roll_number, status
FROM students
WHERE id IN (15, 17, 30);

-- Expected: exactly 3 rows with the names above.
-- If the names don't match, STOP before running Step 2.


-- ===========================================================
-- STEP 2 — DELETE TRANSACTION
-- Run only after verifying the 3 IDs above are correct.
-- The DO block aborts the entire transaction automatically
-- if exactly 3 rows are not found.
-- ===========================================================

BEGIN;

DO $$
DECLARE
    v_ids BIGINT[];
BEGIN
    -- Hardcoded IDs confirmed from production on 2026-06-09
    v_ids := ARRAY[15, 17, 30]::BIGINT[];

    -- Safety check: verify all 3 IDs still exist and names match expectation
    IF (SELECT COUNT(*) FROM students WHERE id = ANY(v_ids)) != 3 THEN
        RAISE EXCEPTION 'One or more of the 3 student IDs (15, 17, 30) not found. Aborting.';
    END IF;

    RAISE NOTICE 'Deleting student IDs: %', v_ids;

    -- -------------------------------------------------------
    -- 1. Library: fines → issues
    -- -------------------------------------------------------
    DELETE FROM library_fines
    WHERE issue_id IN (
        SELECT id FROM library_issues WHERE student_id = ANY(v_ids)
    );

    DELETE FROM library_issues
    WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 2. Exam marks → course_registrations → term_enrollments
    -- -------------------------------------------------------
    DELETE FROM student_marks
    WHERE course_registration_id IN (
        SELECT cr.id
        FROM course_registrations cr
        JOIN student_term_enrollments ste ON ste.id = cr.student_term_enrollment_id
        WHERE ste.student_id = ANY(v_ids)
    );

    -- -------------------------------------------------------
    -- 4. Term fee payments → fee_demands → term_enrollments
    -- -------------------------------------------------------
    DELETE FROM term_fee_payments
    WHERE fee_demand_id IN (
        SELECT fd.id
        FROM fee_demands fd
        JOIN student_term_enrollments ste ON ste.id = fd.student_term_enrollment_id
        WHERE ste.student_id = ANY(v_ids)
    );

    DELETE FROM fee_demands
    WHERE student_term_enrollment_id IN (
        SELECT id FROM student_term_enrollments WHERE student_id = ANY(v_ids)
    );

    -- -------------------------------------------------------
    -- 5. Course registrations → term_enrollments
    -- -------------------------------------------------------
    DELETE FROM course_registrations
    WHERE student_term_enrollment_id IN (
        SELECT id FROM student_term_enrollments WHERE student_id = ANY(v_ids)
    );

    -- -------------------------------------------------------
    -- 6. Term enrollments
    -- -------------------------------------------------------
    DELETE FROM student_term_enrollments WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 7. Penalties (direct student_id FK)
    -- -------------------------------------------------------
    DELETE FROM penalties WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 8. Fee installments (direct student_id FK)
    -- -------------------------------------------------------
    DELETE FROM fee_installments WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 9. installment_fees → student_fee_allocations
    --    (production renamed semester_fees to installment_fees)
    --    fee_installments and penalties already deleted above,
    --    so installment_fees rows are now safe to remove.
    -- -------------------------------------------------------
    DELETE FROM installment_fees
    WHERE allocation_id IN (
        SELECT id FROM student_fee_allocations WHERE student_id = ANY(v_ids)
    );

    -- -------------------------------------------------------
    -- 10. Student fee allocations
    -- -------------------------------------------------------
    DELETE FROM student_fee_allocations WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 10. Legacy fee payments
    -- -------------------------------------------------------
    DELETE FROM fee_payments WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 11. Exam results (legacy table)
    -- -------------------------------------------------------
    DELETE FROM exam_results WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 12. Lab continuous evaluations
    -- -------------------------------------------------------
    DELETE FROM lab_continuous_evaluations WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 13. Attendances
    -- -------------------------------------------------------
    DELETE FROM lab_attendances WHERE student_id = ANY(v_ids);
    DELETE FROM attendances    WHERE student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 16. Admission child records → admissions
    -- -------------------------------------------------------
    DELETE FROM academic_qualifications
    WHERE admission_id IN (
        SELECT id FROM admissions WHERE student_id = ANY(v_ids)
    );

    DELETE FROM admission_documents
    WHERE admission_id IN (
        SELECT id FROM admissions WHERE student_id = ANY(v_ids)
    );

    DELETE FROM admissions WHERE student_id = ANY(v_ids);
    -- Note: enquiry_documents.admission_id and enquiry_document_history.admission_id
    -- are SET NULL automatically via ON DELETE SET NULL when admissions rows are deleted.

    -- -------------------------------------------------------
    -- 17. Null out enquiries.converted_student_id
    --     (no ON DELETE CASCADE — must be cleared manually)
    -- -------------------------------------------------------
    UPDATE enquiries
    SET converted_student_id = NULL
    WHERE converted_student_id = ANY(v_ids);

    -- -------------------------------------------------------
    -- 18. Delete the student rows
    --     The following are handled automatically by DB constraints:
    --       • student_scholarship_eligibility  (ON DELETE CASCADE)
    --       • student_scholarships             (ON DELETE CASCADE)
    --       • student_program_transfers        (ON DELETE CASCADE)
    --       • app_users.student_id             (ON DELETE SET NULL)
    --       • enquiries.referred_student_id    (ON DELETE SET NULL)
    -- -------------------------------------------------------
    DELETE FROM students WHERE id = ANY(v_ids);

    RAISE NOTICE 'Done. % student records and all related data deleted.', array_length(v_ids, 1);
END;
$$;

-- If everything looks correct in the NOTICE output, run:
--   COMMIT;
-- To undo everything instead, run:
--   ROLLBACK;