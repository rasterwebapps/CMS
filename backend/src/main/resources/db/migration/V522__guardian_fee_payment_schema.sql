-- ============================================================
-- V522: Guardian ward fee self-service + Razorpay online payment (OC-254)
-- ============================================================
-- Adds ward-fee self-service permissions (separate VIEW/PAY per the
-- operation-wise permission mapping hard gate -- never reuse one permission
-- for two distinct operations) and an order-lifecycle tracking table for
-- Razorpay. The actual money ledger stays payment_receipts/fee_installments
-- (via PaymentMode.ONLINE_RAZORPAY, a Java-enum-only addition, no column
-- change needed there) -- razorpay_orders below is pre-capture bookkeeping
-- only, so the webhook can resolve an incoming razorpay_order_id back to a
-- student/amount and detect a duplicate delivery before creating a receipt.
-- ============================================================

CREATE TABLE razorpay_orders (
    id                BIGSERIAL PRIMARY KEY,
    razorpay_order_id VARCHAR(64) NOT NULL UNIQUE,
    student_id        BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    guardian_id       BIGINT REFERENCES guardians(id) ON DELETE SET NULL,
    amount            NUMERIC(12,2) NOT NULL,
    currency          VARCHAR(3) NOT NULL DEFAULT 'INR',
    status            VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    razorpay_payment_id VARCHAR(64) UNIQUE,
    receipt_number    VARCHAR(50),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_razorpay_orders_student_id ON razorpay_orders (student_id);

-- Self-service ward fee permissions -- VIEW (summary/receipts/penalties) and
-- PAY (create a Razorpay order) are separate operations, separate rows.
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('MY_WARD_FEE_VIEW', 'View Ward Fee Status', 'FINANCE', 'My Wards', 4, CURRENT_TIMESTAMP),
    ('MY_WARD_FEE_PAY',  'Pay Ward Fees',        'FINANCE', 'My Wards', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'PARENT'
  AND p.code IN ('MY_WARD_FEE_VIEW', 'MY_WARD_FEE_PAY')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
