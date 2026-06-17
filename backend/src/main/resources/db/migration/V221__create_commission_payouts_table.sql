-- ============================================================================
-- V221: Unified commission_payouts table
--
-- Replaces agent_commission_payouts (which only supported agent_id) with a
-- new table that covers all referral source types: Agent, StaffReferrer, Faculty.
--
-- Also adds PAYMENT_REQUESTED to commission_payment_status lifecycle.
-- (VARCHAR column — no ALTER TYPE needed.)
-- ============================================================================

CREATE TABLE commission_payouts (
    id                    BIGSERIAL PRIMARY KEY,
    enquiry_id            BIGINT NOT NULL REFERENCES enquiries(id),
    agent_id              BIGINT REFERENCES agents(id),
    staff_referrer_id     BIGINT REFERENCES staff_referrers(id),
    referred_faculty_id   BIGINT REFERENCES faculty(id),
    amount                NUMERIC(12, 2) NOT NULL,
    payout_date           DATE NOT NULL,
    payment_mode          VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(255),
    remarks               TEXT,
    paid_by               VARCHAR(255),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cp_enquiry ON commission_payouts(enquiry_id);
CREATE INDEX idx_cp_agent   ON commission_payouts(agent_id);
CREATE INDEX idx_cp_staff   ON commission_payouts(staff_referrer_id);
CREATE INDEX idx_cp_faculty ON commission_payouts(referred_faculty_id);

-- Migrate existing agent payout records
INSERT INTO commission_payouts (
    enquiry_id, agent_id, amount, payout_date, payment_mode,
    transaction_reference, remarks, paid_by, created_at, updated_at
)
SELECT
    enquiry_id, agent_id, amount, payout_date, payment_mode,
    transaction_reference, remarks, paid_by, created_at, updated_at
FROM agent_commission_payouts;

DROP TABLE agent_commission_payouts;
