-- Retires the legacy InventoryItem (lab consumables) feature entirely. It was never actually
-- completed/used: 0 rows in inventory_items, 0 VirtualLocation markers of entityType
-- 'INVENTORY_ITEM' anywhere -- confirmed by the user, then verified directly against the DB
-- before writing this migration. The Product/StockBalance system built out over the prior two
-- days' work fully supersedes what this screen was meant to do. See
-- docs/inventory-management/DECISION_LOG.md's 2026-09-09 "Legacy InventoryItem retirement" entry
-- for the full reasoning (this is a retirement, not a data migration -- there was nothing to move).
--
-- Idempotent: IF EXISTS on the DROP, and the permission DELETE only ever removes rows that match
-- these exact codes, so a re-run (or an environment where this was already applied) is a safe
-- no-op rather than an error.

-- 1. role_permissions rows for the 6 legacy permissions must go first (FK to permissions.id).
DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code IN (
    'INVENTORY_VIEW', 'INVENTORY_CREATE', 'INVENTORY_EDIT',
    'INVENTORY_DELETE', 'INVENTORY_EXPORT', 'INVENTORY_MANAGE'
));

-- 2. The permission rows themselves. (Verified before writing this: 0 approval_workflow_steps
--    rows reference any of these 6 codes, so no other FK blocks this delete.)
DELETE FROM permissions
WHERE code IN (
    'INVENTORY_VIEW', 'INVENTORY_CREATE', 'INVENTORY_EDIT',
    'INVENTORY_DELETE', 'INVENTORY_EXPORT', 'INVENTORY_MANAGE'
);

-- 3. The table itself -- empty everywhere it's been checked, and nothing has an FK into it
--    (inventory_items only ever had an outbound FK to labs, never an inbound one).
DROP TABLE IF EXISTS inventory_items;
