-- ============================================================================
--  OC-292: reconcile drifted student_fee_allocations.total_fee / net_fee and
--  has_hostel_fee with SUM(installment_fees.amount) and students.student_type
--  for the 6 remaining boarding-switch-affected students on live 209:
--    - student 194 (2023650006, ASHIK REJO R,       alloc  98): 660000.00 -> 630000.00, has_hostel_fee -> false
--    - student 198 (2023650010, DEVARAJ V.K,        alloc 102): 660000.00 -> 630000.00, has_hostel_fee -> false
--    - student 199 (2023650011, GEOLIN M.S,         alloc 103): 700000.00 -> 670000.00, has_hostel_fee -> false
--    - student 227 (2023650023, MOHAMED HALITH M,   alloc 131): 660000.00 -> 630000.00, has_hostel_fee -> false
--    - student 255 (2023650038, SURENDHAR . M,      alloc 159): 800000.00 -> 770000.00, has_hostel_fee -> false
--    - student 259 (2023650042, VASU S,             alloc 163): 700000.00 -> 670000.00, has_hostel_fee -> false
--  (Note: students 108, 228, and 234 from the initial 9-student report were
--   already reconciled in OC-288, OC-289, and OC-291.)
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only): all 9 students from the drift report
SELECT a.id AS alloc_id, s.id AS student_id, s.admission_number,
       concat_ws(' ', s.first_name, s.last_name) AS student_name, s.student_type,
       a.has_hostel_fee, a.total_fee, a.discount_amount, a.net_fee,
       COALESCE(SUM(i.amount), 0) AS installments_sum,
       a.net_fee - COALESCE(SUM(i.amount), 0) AS net_drift
FROM student_fee_allocations a
JOIN students s ON s.id = a.student_id
LEFT JOIN installment_fees i ON i.allocation_id = a.id
WHERE s.id IN (108, 194, 198, 199, 227, 228, 234, 255, 259)
GROUP BY a.id, s.id, s.admission_number, s.first_name, s.last_name, s.student_type, a.has_hostel_fee, a.total_fee, a.discount_amount, a.net_fee
ORDER BY s.id;

-- Update the 6 remaining drifted allocations (98, 102, 103, 131, 159, 163)
UPDATE student_fee_allocations a
   SET net_fee = sub.inst_sum,
       total_fee = sub.inst_sum + COALESCE(a.discount_amount, 0.00),
       has_hostel_fee = (s.student_type = 'HOSTELER'),
       updated_at = now()
  FROM students s,
       (SELECT allocation_id, SUM(amount) AS inst_sum
          FROM installment_fees
         WHERE allocation_id IN (98, 102, 103, 131, 159, 163)
         GROUP BY allocation_id) sub
 WHERE a.id = sub.allocation_id
   AND s.id = a.student_id
   AND a.id IN (98, 102, 103, 131, 159, 163)
   AND a.student_id IN (194, 198, 199, 227, 255, 259)
   AND a.net_fee - sub.inst_sum = 30000.00;

-- In-transaction invariant assertions (abort and rollback on any discrepancy)
DO $$
DECLARE
    v_drifted_count integer;
BEGIN
    SELECT COUNT(*) INTO v_drifted_count
      FROM student_fee_allocations a
     WHERE a.net_fee <> (SELECT COALESCE(SUM(i.amount), 0) FROM installment_fees i WHERE i.allocation_id = a.id)
        OR a.total_fee <> (SELECT COALESCE(SUM(i.amount), 0) FROM installment_fees i WHERE i.allocation_id = a.id) + COALESCE(a.discount_amount, 0);

    IF v_drifted_count <> 0 THEN
        RAISE EXCEPTION 'Aborted: % student_fee_allocations row(s) still have total_fee/net_fee drift', v_drifted_count;
    END IF;
END $$;

-- Post-check (read-only): all 9 students from the drift report
SELECT a.id AS alloc_id, s.id AS student_id, s.admission_number,
       concat_ws(' ', s.first_name, s.last_name) AS student_name, s.student_type,
       a.has_hostel_fee, a.total_fee, a.discount_amount, a.net_fee,
       COALESCE(SUM(i.amount), 0) AS installments_sum,
       a.net_fee - COALESCE(SUM(i.amount), 0) AS net_drift,
       COALESCE((SELECT SUM(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = s.id AND fi.refunded_at IS NULL), 0) AS total_paid,
       a.net_fee - COALESCE((SELECT SUM(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = s.id AND fi.refunded_at IS NULL), 0) AS outstanding
FROM student_fee_allocations a
JOIN students s ON s.id = a.student_id
LEFT JOIN installment_fees i ON i.allocation_id = a.id
WHERE s.id IN (108, 194, 198, 199, 227, 228, 234, 255, 259)
GROUP BY a.id, s.id, s.admission_number, s.first_name, s.last_name, s.student_type, a.has_hostel_fee, a.total_fee, a.discount_amount, a.net_fee
ORDER BY s.id;

COMMIT;
