-- Fee Refunds: reversal records that restore outstanding balance without deleting original receipts.
-- The refunded installment rows in fee_installments are soft-flagged (refunded_at / refund_number)
-- so the outstanding calculation can exclude them while the original receipt stays intact.

CREATE TABLE fee_refunds (
    id               BIGSERIAL        PRIMARY KEY,
    refund_number    VARCHAR(50)      NOT NULL UNIQUE,
    original_receipt_number VARCHAR(50) NOT NULL,
    student_id       BIGINT           NOT NULL,
    student_name     VARCHAR(255)     NOT NULL,
    roll_number      VARCHAR(50),
    admission_number VARCHAR(20),
    program_name     VARCHAR(255),
    refund_amount    NUMERIC(12, 2)   NOT NULL,
    refund_date      DATE             NOT NULL,
    reason           TEXT             NOT NULL,
    created_by       VARCHAR(100),
    created_at       TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- Soft-flag on fee_installments — refunded rows are excluded from outstanding calculations.
ALTER TABLE fee_installments
    ADD COLUMN IF NOT EXISTS refunded_at    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS refund_number  VARCHAR(50);

-- FEE_REFUND permission
INSERT INTO permissions (code, display_name, category, description, created_at)
VALUES ('FEE_REFUND', 'Fee Refund', 'FINANCE', 'Issue a reversal against an existing student payment receipt', NOW())
ON CONFLICT (code) DO NOTHING;

-- Sync DEV_ADMIN and SUPPORT_ADMIN to hold every permission (catches all gaps since V129)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
