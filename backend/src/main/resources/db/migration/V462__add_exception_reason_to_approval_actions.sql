-- Inventory Management (Release 3) — Phase 6 "Budgets & Approvals" third and final slice:
-- Exception handling with documented reasons. Adds a structured exception_reason directly to
-- the already-shipped approval_actions table (V460) rather than a new child table — a bypass is
-- a distinct way of resolving the same action row, not its own document. New forward migration
-- on an already-shipped table; V460 itself untouched. See docs/inventory-management/
-- DECISION_LOG.md's "Exception handling slice" entry.

ALTER TABLE approval_actions ADD COLUMN exception_reason VARCHAR(40);
ALTER TABLE approval_actions ADD CONSTRAINT chk_approval_actions_exception_reason
    CHECK (exception_reason IS NULL OR exception_reason IN (
        'URGENT_PURCHASE', 'SINGLE_SUPPLIER_SITUATION', 'EMERGENCY', 'APPROVER_UNAVAILABLE', 'OTHER'
    ));
