-- ============================================================
-- V259: Excess-payment auto-refund support
-- ============================================================
-- 1. Adds `source` to fee_refunds so a refund can be tagged as
--    system-auto-generated (from an over-outstanding bank payment)
--    vs. staff-initiated. AUTO_EXCESS refunds cannot be rejected/
--    deleted by staff (enforced in FeeRefundService, not the DB).
-- 2. Seeds FEE_COLLECT_EXCESS permission gating the "allow excess
--    payment" checkbox on bank-transfer/DD advance payments.
-- ============================================================

-- ── 1. Schema ─────────────────────────────────────────────────────────────────
ALTER TABLE fee_refunds
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- ── 2. Permission ────────────────────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at)
VALUES ('FEE_COLLECT_EXCESS', 'Collect Excess Payment (Bank)', 'FINANCE', 'Collect Payment', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ── 3. DEV_ADMIN / SUPPORT_ADMIN catch-all sync ─────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
