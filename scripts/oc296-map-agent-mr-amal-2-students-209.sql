-- ============================================================================
--  OC-296: Map Agent 'Mr Amal' (id 1) for JISIN M.J (adm 2025650012)
--  and SWETHA SANTHOSH (adm 2025650061) on live 209.
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Scope:
--    1. enquiries (id 44, student 13, JISIN M.J):
--         agent_id: NULL -> 1 ('Mr Amal')
--         referral_type_id: 1 ('Direct') -> 4 ('Agent')
--         commission_source: NULL -> 'AGENT'
--         commission_amount: NULL -> 50000.00
--         commission_paid_amount: 0.00 -> 50000.00
--         commission_payment_status: 'NOT_APPLICABLE' -> 'PAID'
--    2. enquiries (id 105, student 90, SWETHA SANTHOSH):
--         agent_id: NULL -> 1 ('Mr Amal')
--         referral_type_id: 29 ('KERALA AGENT') -> 4 ('Agent')
--         commission_source: 'AGENT' (already set)
--         commission_amount: 50000.00 (already set)
--         commission_paid_amount: 50000.00 (already set)
--         commission_payment_status: 'PAID' (already set)
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only)
SELECT e.id AS enquiry_id, s.id AS student_id, s.admission_number,
       s.first_name || ' ' || s.last_name AS student_name,
       e.agent_id, ag.name AS agent_name, e.referral_type_id, rt.name AS referral_type_name,
       e.commission_amount, e.commission_paid_amount, e.commission_payment_status
FROM enquiries e
JOIN students s ON s.id = e.converted_student_id
LEFT JOIN agents ag ON ag.id = e.agent_id
LEFT JOIN referral_types rt ON rt.id = e.referral_type_id
WHERE s.admission_number IN ('2025650012', '2025650061')
ORDER BY s.admission_number;

-- 1. Update Enquiry 44 (JISIN M.J)
UPDATE enquiries
   SET agent_id = 1,
       referral_type_id = 4,
       commission_source = 'AGENT',
       commission_amount = 50000.00,
       commission_paid_amount = 50000.00,
       commission_payment_status = 'PAID',
       updated_at = now()
 WHERE id = 44
   AND converted_student_id = 13
   AND agent_id IS NULL;

-- 2. Update Enquiry 105 (SWETHA SANTHOSH)
UPDATE enquiries
   SET agent_id = 1,
       referral_type_id = 4,
       commission_source = 'AGENT',
       updated_at = now()
 WHERE id = 105
   AND converted_student_id = 90
   AND agent_id IS NULL;

-- In-transaction invariant assertions
DO $$
DECLARE
    v_count int;
BEGIN
    SELECT count(*) INTO v_count
      FROM enquiries e
      JOIN students s ON s.id = e.converted_student_id
     WHERE s.admission_number IN ('2025650012', '2025650061')
       AND e.agent_id = 1
       AND e.referral_type_id = 4
       AND e.commission_source = 'AGENT'
       AND e.commission_amount = 50000.00
       AND e.commission_payment_status = 'PAID';

    IF v_count <> 2 THEN
        RAISE EXCEPTION 'Aborted: expected 2 enquiries mapped to agent 1, got %', v_count;
    END IF;
END $$;

-- Post-check (read-only)
SELECT e.id AS enquiry_id, s.id AS student_id, s.admission_number,
       s.first_name || ' ' || s.last_name AS student_name,
       e.agent_id, ag.name AS agent_name, ag.phone AS agent_phone,
       e.referral_type_id, rt.name AS referral_type_name,
       e.commission_amount, e.commission_paid_amount, e.commission_payment_status
FROM enquiries e
JOIN students s ON s.id = e.converted_student_id
JOIN agents ag ON ag.id = e.agent_id
JOIN referral_types rt ON rt.id = e.referral_type_id
WHERE s.admission_number IN ('2025650012', '2025650061')
ORDER BY s.admission_number;

COMMIT;
