-- Backfill: apply existing enquiry payment amounts as credits to fee demands.
-- Applied chronologically by due_date (earliest demand first) per business rule.
-- Surplus (credit exceeding all demands) is left unapplied — cashier handles manually.

DO $$
DECLARE
    r          RECORD;
    v_total    NUMERIC(12,2);
    v_remaining NUMERIC(12,2);
    d          RECORD;
    v_apply    NUMERIC(12,2);
BEGIN
    FOR r IN
        SELECT a.enquiry_id, a.student_id
        FROM admissions a
        WHERE a.enquiry_id IS NOT NULL
    LOOP
        SELECT COALESCE(SUM(ep.amount_paid), 0)
        INTO v_total
        FROM enquiry_payments ep
        WHERE ep.enquiry_id = r.enquiry_id;

        CONTINUE WHEN v_total = 0;
        v_remaining := v_total;

        FOR d IN
            SELECT fd.id, fd.total_amount, fd.paid_amount
            FROM fee_demands fd
            JOIN student_term_enrollments ste ON ste.id = fd.student_term_enrollment_id
            WHERE ste.student_id = r.student_id
            ORDER BY fd.due_date ASC
        LOOP
            EXIT WHEN v_remaining <= 0;

            v_apply := LEAST(v_remaining, d.total_amount - d.paid_amount);
            CONTINUE WHEN v_apply <= 0;

            UPDATE fee_demands
            SET paid_amount = paid_amount + v_apply,
                status      = CASE
                                  WHEN paid_amount + v_apply >= total_amount THEN 'PAID'
                                  WHEN paid_amount + v_apply > 0             THEN 'PARTIAL'
                                  ELSE status
                              END,
                updated_at  = NOW()
            WHERE id = d.id;

            v_remaining := v_remaining - v_apply;
        END LOOP;
    END LOOP;
END $$;
