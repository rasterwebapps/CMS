-- ============================================================================
-- V79: Commission tracking & fee-collection upgrades
--
-- 1. Decouple agent / referral commission from the student-facing fee.
--    Commission is now stored separately on the enquiry and tracked through
--    its own payout lifecycle (PENDING → PARTIAL → PAID) instead of being
--    silently added into `final_calculated_fee` / `finalized_total_fee`.
--
-- 2. Track per-receipt split of student fee vs commission component on
--    enquiry_payments so that historical receipts remain reconcilable even
--    after we begin paying commissions to agents.
--
-- 3. New table `agent_commission_payouts` records every payment made to an
--    agent (or other commission recipient) against a specific enquiry.
--
-- Backfill strategy: Option B — historical rows keep their existing
-- finalized_total_fee / referral_additional_amount untouched.  Going forward
-- the new rules apply.  We DO populate commission_amount / source for
-- existing enquiries from `referral_additional_amount` so that summaries are
-- still meaningful.
-- ============================================================================

ALTER TABLE enquiries ADD COLUMN commission_amount NUMERIC(12, 2);
ALTER TABLE enquiries ADD COLUMN commission_source VARCHAR(20);
ALTER TABLE enquiries ADD COLUMN commission_paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE enquiries ADD COLUMN commission_payment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE';

-- Backfill for existing rows: derive commission from previously stored
-- referral_additional_amount.  Mark source as REFERRAL_TYPE for any row that
-- had a non-zero amount; AGENT if an agent is also linked, NONE otherwise.
UPDATE enquiries
   SET commission_amount         = COALESCE(referral_additional_amount, 0),
       commission_source         = CASE
                                       WHEN agent_id IS NOT NULL AND COALESCE(referral_additional_amount, 0) > 0 THEN 'AGENT'
                                       WHEN COALESCE(referral_additional_amount, 0) > 0 THEN 'REFERRAL_TYPE'
                                       ELSE 'NONE'
                                   END,
       commission_payment_status = CASE
                                       WHEN COALESCE(referral_additional_amount, 0) > 0 THEN 'PENDING'
                                       ELSE 'NOT_APPLICABLE'
                                   END;

-- ---------------------------------------------------------------------------
-- Per-receipt split on enquiry_payments
-- ---------------------------------------------------------------------------
ALTER TABLE enquiry_payments ADD COLUMN student_fee_component NUMERIC(12, 2);
ALTER TABLE enquiry_payments ADD COLUMN commission_component  NUMERIC(12, 2) NOT NULL DEFAULT 0;

-- Default existing receipts: the entire amount belongs to the student fee
-- (no commission was ever deducted from receipts under the old model).
UPDATE enquiry_payments SET student_fee_component = amount_paid WHERE student_fee_component IS NULL;

-- ---------------------------------------------------------------------------
-- New: agent commission payouts ledger
-- ---------------------------------------------------------------------------
CREATE TABLE agent_commission_payouts (
    id                    BIGSERIAL PRIMARY KEY,
    enquiry_id            BIGINT NOT NULL REFERENCES enquiries(id),
    agent_id              BIGINT REFERENCES agents(id),
    amount                NUMERIC(12, 2) NOT NULL,
    payout_date           DATE NOT NULL,
    payment_mode          VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(255),
    remarks               TEXT,
    paid_by               VARCHAR(255),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_acp_enquiry ON agent_commission_payouts(enquiry_id);
CREATE INDEX idx_acp_agent   ON agent_commission_payouts(agent_id);

