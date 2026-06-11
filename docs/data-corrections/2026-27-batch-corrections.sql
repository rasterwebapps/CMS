-- =============================================================================
-- SKSCMS DATA CORRECTION SCRIPT — 2026-27 BATCH
-- Date       : 2026-06-08
-- =============================================================================
-- BEFORE RUNNING:
--   pg_dump -h 172.16.7.209 -p 5433 -U <user> cms > backup_20260608.sql
--
-- ENQUIRY DELETIONS (confirmed IDs):
--   id=13  BOSE.R          FEES_FINALIZED
--   id=16  DHARSHINI.D     FEES_FINALIZED
--   id=15  DHARSHINI.G     FEES_FINALIZED
--   id=55  N.MARIYA        PARTIALLY_PAID  ← has advance payments
--   id=6   JERSLIN.S       ENQUIRED
--   id=17  SUDEEP.R        FEES_FINALIZED
--   id=21  NITHIN PAUL.P   FEES_FINALIZED
--
-- FULL STUDENT DELETION:
--   Hari — student id=11 (HARIPRASATH M)
--
-- ENQUIRY DUPLICATE DELETION:
--   id=24  SHEBIN G SANTHOSH   PARTIALLY_PAID  2026-05-30  ← DELETE this
--   id=29  SHEBIN.G.SANTHOSH   PARTIALLY_PAID  2026-06-02  ← KEEP this
-- =============================================================================


-- =============================================================================
-- SECTION 0 — LOOKUP: all IDs confirmed, kept for reference only
-- =============================================================================
-- Enquiries to delete : ids 6, 13, 15, 16, 17, 21, 24, 55
-- Student to delete   : id=11 (HARIPRASATH M)
-- Enquiry to keep     : id=29 (SHEBIN.G.SANTHOSH)


-- =============================================================================
-- SECTION 1 — ENQUIRY DELETIONS (IDs confirmed, safe to run)
-- =============================================================================

BEGIN;
-- BOSE.R (id=13)
DELETE FROM agent_commission_payouts WHERE enquiry_id = 13;
DELETE FROM enquiry_payments         WHERE enquiry_id = 13;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 13;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 13;
DELETE FROM enquiry_documents        WHERE enquiry_id = 13;
DELETE FROM enquiries                WHERE id = 13;
ROLLBACK;

BEGIN;
-- DHARSHINI.D (id=16)
DELETE FROM agent_commission_payouts WHERE enquiry_id = 16;
DELETE FROM enquiry_payments         WHERE enquiry_id = 16;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 16;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 16;
DELETE FROM enquiry_documents        WHERE enquiry_id = 16;
DELETE FROM enquiries                WHERE id = 16;
COMMIT;

BEGIN;
-- DHARSHINI.G (id=15)
DELETE FROM agent_commission_payouts WHERE enquiry_id = 15;
DELETE FROM enquiry_payments         WHERE enquiry_id = 15;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 15;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 15;
DELETE FROM enquiry_documents        WHERE enquiry_id = 15;
DELETE FROM enquiries                WHERE id = 15;
COMMIT;

BEGIN;
-- N.MARIYA (id=55) — has partial payments, handled below
DELETE FROM agent_commission_payouts WHERE enquiry_id = 55;
DELETE FROM enquiry_payments         WHERE enquiry_id = 55;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 55;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 55;
DELETE FROM enquiry_documents        WHERE enquiry_id = 55;
DELETE FROM enquiries                WHERE id = 55;
COMMIT;

BEGIN;
-- JERSLIN.S (id=6)
DELETE FROM agent_commission_payouts WHERE enquiry_id = 6;
DELETE FROM enquiry_payments         WHERE enquiry_id = 6;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 6;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 6;
DELETE FROM enquiry_documents        WHERE enquiry_id = 6;
DELETE FROM enquiries                WHERE id = 6;
COMMIT;

BEGIN;
-- SUDEEP.R (id=17)
DELETE FROM agent_commission_payouts WHERE enquiry_id = 17;
DELETE FROM enquiry_payments         WHERE enquiry_id = 17;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 17;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 17;
DELETE FROM enquiry_documents        WHERE enquiry_id = 17;
DELETE FROM enquiries                WHERE id = 17;
COMMIT;

BEGIN;
-- NITHIN PAUL.P (id=21)
DELETE FROM agent_commission_payouts WHERE enquiry_id = 21;
DELETE FROM enquiry_payments         WHERE enquiry_id = 21;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 21;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 21;
DELETE FROM enquiry_documents        WHERE enquiry_id = 21;
DELETE FROM enquiries                WHERE id = 21;
COMMIT;


-- =============================================================================
-- SECTION 2 — HARI: Full student deletion (id=11, HARIPRASATH M)
-- =============================================================================

BEGIN;
DO $$ DECLARE v BIGINT := 11;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM students WHERE id = v) THEN
        RAISE EXCEPTION 'Student id=% not found — aborting.', v;
    END IF;
    RAISE NOTICE 'Deleting student id=% — Hari', v;
    DELETE FROM term_fee_payments WHERE fee_demand_id IN (SELECT fd.id FROM fee_demands fd JOIN student_term_enrollments ste ON ste.id = fd.student_term_enrollment_id WHERE ste.student_id = v);
    DELETE FROM term_results       WHERE student_term_enrollment_id IN (SELECT id FROM student_term_enrollments WHERE student_id = v);
    DELETE FROM student_marks      WHERE course_registration_id IN (SELECT cr.id FROM course_registrations cr JOIN student_term_enrollments ste ON ste.id = cr.student_term_enrollment_id WHERE ste.student_id = v);
    DELETE FROM course_registrations WHERE student_term_enrollment_id IN (SELECT id FROM student_term_enrollments WHERE student_id = v);
    DELETE FROM fee_demands        WHERE student_term_enrollment_id IN (SELECT id FROM student_term_enrollments WHERE student_id = v);
    DELETE FROM student_term_enrollments WHERE student_id = v;
    DELETE FROM fee_installments   WHERE semester_fee_id IN (SELECT sf.id FROM installment_fees sf JOIN student_fee_allocations sfa ON sfa.id = sf.allocation_id WHERE sfa.student_id = v);
    DELETE FROM installment_fees   WHERE allocation_id IN (SELECT id FROM student_fee_allocations WHERE student_id = v);
    DELETE FROM fee_payments       WHERE student_id = v;
    DELETE FROM student_fee_allocations WHERE student_id = v;
    DELETE FROM penalties          WHERE student_id = v;
    DELETE FROM lab_continuous_evaluations WHERE student_id = v;
    DELETE FROM lab_attendances    WHERE student_id = v;
    DELETE FROM attendances        WHERE student_id = v;
    DELETE FROM exam_results       WHERE student_id = v;
    DELETE FROM library_fines      WHERE issue_id IN (SELECT id FROM library_issues WHERE student_id = v);
    DELETE FROM library_issues     WHERE student_id = v;
    DELETE FROM payment_receipts   WHERE payer_type = 'STUDENT' AND payer_id = v;
    DELETE FROM admissions         WHERE student_id = v;
    UPDATE enquiries SET converted_student_id = NULL WHERE converted_student_id = v;
    DELETE FROM students WHERE id = v;
    RAISE NOTICE 'Done — Hari deleted.';
END $$;
COMMIT;


-- =============================================================================
-- SECTION 3 — SHEBIN: Delete duplicate enquiry id=24 (2026-05-30)
-- Keeping id=29 (SHEBIN.G.SANTHOSH, 2026-06-02)
-- =============================================================================

BEGIN;
DELETE FROM agent_commission_payouts WHERE enquiry_id = 24;
DELETE FROM enquiry_payments         WHERE enquiry_id = 24;
DELETE FROM enquiry_status_history   WHERE enquiry_id = 24;
DELETE FROM payment_receipts         WHERE payer_type = 'ENQUIRY' AND payer_id = 24;
DELETE FROM enquiry_documents        WHERE enquiry_id = 24;
DELETE FROM enquiries                WHERE id = 24;
COMMIT;


-- =============================================================================
-- VERIFICATION
-- =============================================================================
-- SELECT id, name FROM enquiries WHERE id IN (6,13,15,16,17,21,24,55);  -- 0 rows
-- SELECT id FROM students WHERE id = 11;                               -- 0 rows
-- SELECT id, name FROM enquiries WHERE id = 29;                        -- still exists
-- =============================================================================
