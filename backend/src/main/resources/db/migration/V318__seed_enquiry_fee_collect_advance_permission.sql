-- ============================================================
-- V318: Enquiry advance/excess payment support
-- ============================================================
-- Seeds ENQUIRY_FEE_COLLECT_ADVANCE, the dedicated permission gating
-- the "Collect Advance Payment" toggle on the enquiry payment
-- collection dialog (fee-collection.component). Two things it gates:
--   1. Raising the collection cap from "currently open terms only"
--      to the enquiry's full remaining course fee, for any payment
--      mode.
--   2. Within that mode, a further opt-in to exceed even the full
--      course fee for DEMAND_DRAFT/BANK_TRANSFER only, which carves
--      the excess into a non-rejectable AUTO_EXCESS fee_refunds row
--      (entity_type = ENQUIRY) — mirrors FEE_COLLECT_EXCESS (V259)
--      but kept as its own permission since it's a distinct operation
--      (enquiry vs. post-admission student), per the operation-wise
--      permission mapping rule.
-- No schema change needed: fee_refunds.source and entity_type/
-- enquiry_id already exist (V259, base schema).
-- ============================================================

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at)
VALUES ('ENQUIRY_FEE_COLLECT_ADVANCE', 'Collect Advance/Excess Payment (Enquiry)', 'FINANCE', 'Collect Payment', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ── DEV_ADMIN / SUPPORT_ADMIN catch-all sync ────────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND p.code = 'ENQUIRY_FEE_COLLECT_ADVANCE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
