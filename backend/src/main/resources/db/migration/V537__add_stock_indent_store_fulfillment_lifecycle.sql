-- Phase D of the Stock Indent auto-indent feature (see docs/inventory-management/DECISION_LOG.md's
-- 2026-09-21 "OC-206 reopened" entries) — splits what was a single approve-and-issue step into
-- two: department-head approval (APPROVED, unchanged trigger, no longer posts a movement), then a
-- separate store-side fulfillment decision that lands on one of four terminal outcomes:
--   FULFILLED   — store issued the stock directly (same ISSUE movement the old APPROVED used to
--                 post immediately on approval)
--   FULFILLED   — also reached via a transfer-in from another location first (source_transfer_id
--                 records which StockTransfer covered the shortfall); no separate status needed,
--                 the transfer link is what distinguishes the two paths
--   PO_RAISED   — store had no stock anywhere and raised a Purchase Requisition instead
--   DENIED      — store declined to fulfill (distinct from the department head's own REJECTED)
-- Both manual and auto-generated indents share this one lifecycle (user-confirmed decision).
-- New store_decided_* columns are a second, separate audit trail from the existing resolved_*
-- columns (which stay department-head-only) — the two are different actors at different stages.
-- Column names verified against V446/V531 (stock_indent_items), V446 (stock_transfers via
-- V423/V446 — table already exists), and V434 (purchase_requisitions/purchase_requisition_items).

ALTER TABLE stock_indent_items DROP CONSTRAINT chk_stock_indent_items_status;
ALTER TABLE stock_indent_items ADD CONSTRAINT chk_stock_indent_items_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FULFILLED', 'PO_RAISED', 'DENIED'));

ALTER TABLE stock_indent_items ADD COLUMN store_decided_by VARCHAR(255);
ALTER TABLE stock_indent_items ADD COLUMN store_decided_at TIMESTAMPTZ;
ALTER TABLE stock_indent_items ADD COLUMN store_decision_notes VARCHAR(500);
ALTER TABLE stock_indent_items ADD COLUMN source_transfer_id BIGINT REFERENCES stock_transfers(id);
ALTER TABLE stock_indent_items ADD COLUMN raised_requisition_id BIGINT REFERENCES purchase_requisitions(id);
ALTER TABLE stock_indent_items ADD COLUMN raised_requisition_item_id BIGINT REFERENCES purchase_requisition_items(id);
