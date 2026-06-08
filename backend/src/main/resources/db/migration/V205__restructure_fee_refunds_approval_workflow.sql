-- V205: Restructure fee_refunds for two-step approval workflow.
-- The V204 single-step table is dropped and replaced with a full approval schema.
-- Soft-flag columns on fee_installments (refunded_at, refund_number) are unchanged — added by V204.

DROP TABLE IF EXISTS fee_refunds;

CREATE TABLE fee_refunds (
    id                    BIGSERIAL        PRIMARY KEY,
    refund_number         VARCHAR(50)      UNIQUE,              -- NULL until APPROVED; generated on approval
    original_receipt_number VARCHAR(50)    NOT NULL,
    student_id            BIGINT           NOT NULL,
    student_name          VARCHAR(255)     NOT NULL,
    roll_number           VARCHAR(50),
    admission_number      VARCHAR(20),
    program_name          VARCHAR(255),
    refund_amount         NUMERIC(12, 2)   NOT NULL,
    reason                TEXT             NOT NULL,
    status                VARCHAR(20)      NOT NULL DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED
    requested_by          VARCHAR(100),
    requested_at          TIMESTAMP        NOT NULL DEFAULT NOW(),
    -- set on APPROVED
    payment_mode          VARCHAR(30),
    payment_date          DATE,
    transaction_reference VARCHAR(255),
    approved_by           VARCHAR(100),
    approved_at           TIMESTAMP,
    -- set on REJECTED
    rejection_reason      TEXT,
    created_at            TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- Permission to approve or reject pending refund requests
INSERT INTO permissions (code, display_name, category, description, created_at)
VALUES ('FEE_REFUND_APPROVE', 'Approve Fee Refund', 'FINANCE', 'Approve or reject pending fee refund requests', NOW())
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
