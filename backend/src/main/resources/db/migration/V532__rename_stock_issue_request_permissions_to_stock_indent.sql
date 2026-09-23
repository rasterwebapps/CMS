-- Renames the Stock Issue Request permissions to Stock Indent, matching V531's entity/table
-- rename. Updated in place (same permission row/id) rather than inserted as new rows + carried
-- forward via role_permissions — this is the same permission being renamed, not a new distinct
-- capability, so every existing role's grant stays intact automatically with no copy-forward
-- logic needed. Idempotent via WHERE guards so a partial re-run is a no-op.

UPDATE permissions SET code = 'INVENTORY_STOCK_INDENT_VIEW', display_name = 'View Stock Indents', screen_label = 'Stock Indents'
    WHERE code = 'INVENTORY_ISSUE_REQUEST_VIEW';
UPDATE permissions SET code = 'INVENTORY_STOCK_INDENT_MANAGE', display_name = 'Manage Stock Indents', screen_label = 'Stock Indents'
    WHERE code = 'INVENTORY_ISSUE_REQUEST_MANAGE';
UPDATE permissions SET code = 'INVENTORY_STOCK_INDENT_APPROVE', display_name = 'Approve Stock Indents', screen_label = 'Stock Indents'
    WHERE code = 'INVENTORY_ISSUE_REQUEST_APPROVE';
UPDATE permissions SET code = 'INVENTORY_STOCK_INDENT_RETURN', display_name = 'Return Issued Stock (Indent)', screen_label = 'Stock Indents'
    WHERE code = 'INVENTORY_ISSUE_REQUEST_RETURN';
