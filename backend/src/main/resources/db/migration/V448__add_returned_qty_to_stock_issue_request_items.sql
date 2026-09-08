-- Inventory Management (Release 3) — Phase 4 "Requests, Issues & Returns" second slice: Internal
-- Return. Adds a running returned_qty total directly on stock_issue_request_items (same "running
-- total on the line itself" shape as purchase_order_items.received_qty from V438) rather than a
-- new child table — a return is a simple accumulating action against an already-issued line, not
-- its own multi-line document. See docs/inventory-management/DECISION_LOG.md's "Internal Return
-- slice" entry. New forward migration on an already-shipped table (V446) — V446 itself untouched.

ALTER TABLE stock_issue_request_items ADD COLUMN returned_qty NUMERIC(14,3) NOT NULL DEFAULT 0;
ALTER TABLE stock_issue_request_items ADD CONSTRAINT chk_stock_issue_request_items_returned_qty
    CHECK (returned_qty >= 0 AND returned_qty <= requested_qty);
