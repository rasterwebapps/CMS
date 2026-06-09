-- V208: Normalize term_fee_payments receipt numbers to RCP-YYYY-NNNNN format
-- and backfill them into the unified payment_receipts ledger.
--
-- Step 1: Reassign receipt numbers on existing term_fee_payments per year,
--         starting after the current max sequence for that year.
-- Step 2: Backfill payment_receipts from term_fee_payments.

DO $$
DECLARE
    v_year     INTEGER;
    v_last_seq INTEGER;
    v_new_seq  INTEGER;
    v_rcp_num  VARCHAR(50);
    v_payment  RECORD;
BEGIN
    -- Process each year that has term_fee_payments
    FOR v_year IN
        SELECT DISTINCT EXTRACT(YEAR FROM payment_date)::INTEGER AS yr
        FROM term_fee_payments
        ORDER BY yr
    LOOP
        -- Get the current last_sequence for RECEIPT_NUMBER + this year (0 if no row yet)
        SELECT COALESCE(
            (SELECT last_sequence FROM application_number_sequences
             WHERE series_code = 'RECEIPT_NUMBER' AND scope_key = v_year::TEXT),
            (SELECT COALESCE(last_seq, 0) FROM receipt_number_sequence WHERE year = v_year),
            0
        ) INTO v_last_seq;

        v_new_seq := v_last_seq;

        -- Assign new receipt numbers in payment_date + id order
        FOR v_payment IN
            SELECT id
            FROM term_fee_payments
            WHERE EXTRACT(YEAR FROM payment_date)::INTEGER = v_year
            ORDER BY payment_date, id
        LOOP
            v_new_seq := v_new_seq + 1;
            v_rcp_num := 'RCP-' || v_year || '-' || lpad(v_new_seq::TEXT, 5, '0');

            UPDATE term_fee_payments SET receipt_number = v_rcp_num WHERE id = v_payment.id;
        END LOOP;

        -- Update (or insert) the sequence counter for RECEIPT_NUMBER + this year
        INSERT INTO application_number_sequences
            (series_code, series_name, scope_type, scope_key, prefix, sequence_padding, last_sequence, description)
        VALUES
            ('RECEIPT_NUMBER', 'Receipt Number', 'CALENDAR_YEAR', v_year::TEXT,
             'RCP', 5, v_new_seq,
             'Global receipt number generated for every payment receipt')
        ON CONFLICT (series_code, scope_key) DO UPDATE SET last_sequence = v_new_seq;

        -- Keep the legacy receipt_number_sequence table in sync if the row exists
        UPDATE receipt_number_sequence SET last_seq = v_new_seq WHERE year = v_year;
    END LOOP;
END $$;

-- Step 2: Backfill unified ledger from term_fee_payments
INSERT INTO payment_receipts (
    receipt_number, payer_type, payer_id, payer_name, payer_identifier,
    admission_number, program_name, amount_paid, payment_date, payment_mode,
    transaction_reference, remarks, installments_covered, collected_by, fee_category, created_at
)
SELECT
    tfp.receipt_number,
    'STUDENT',
    s.id,
    s.first_name || ' ' || s.last_name,
    s.roll_number,
    s.admission_number,
    COALESCE(c.name, p.name),
    tfp.amount_paid + COALESCE(tfp.late_fee_applied, 0),
    tfp.payment_date,
    tfp.payment_mode::VARCHAR,
    tfp.transaction_reference,
    tfp.remarks,
    ti.term_type || ' - ' || ay.name,
    NULL,
    NULL,
    tfp.created_at
FROM term_fee_payments tfp
JOIN fee_demands fd                    ON fd.id                        = tfp.fee_demand_id
JOIN student_term_enrollments ste      ON ste.id                       = fd.student_term_enrollment_id
JOIN students s                        ON s.id                         = ste.student_id
JOIN term_instances ti                 ON ti.id                        = fd.term_instance_id
JOIN academic_years ay                 ON ay.id                        = ti.academic_year_id
LEFT JOIN courses  c                   ON c.id                         = s.course_id
LEFT JOIN programs p                   ON p.id                         = s.program_id
WHERE NOT EXISTS (
    SELECT 1 FROM payment_receipts pr WHERE pr.receipt_number = tfp.receipt_number
);
