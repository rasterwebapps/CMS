-- Renames Stock Issue Request -> Stock Indent, per the user's own domain terminology (see
-- docs/inventory-management/DECISION_LOG.md's "Auto-restocking (OC-206) skipped" entry — this is
-- that deferred feature being picked back up, and the user confirmed "Indent" and "Stock Issue
-- Request" are the same concept, not two separate documents). Pure rename: no data loss, no
-- column type/shape change. New forward migration on already-shipped tables (V446/V448/V500) —
-- those files stay untouched. All constraint/index/sequence names below were verified against the
-- live local dev DB (psql \d) before writing this, not guessed.

ALTER TABLE stock_issue_requests RENAME TO stock_indents;
ALTER TABLE stock_issue_request_items RENAME TO stock_indent_items;
ALTER TABLE stock_indent_items RENAME COLUMN stock_issue_request_id TO stock_indent_id;

ALTER SEQUENCE stock_issue_requests_id_seq RENAME TO stock_indents_id_seq;
ALTER SEQUENCE stock_issue_request_items_id_seq RENAME TO stock_indent_items_id_seq;

ALTER TABLE stock_indents RENAME CONSTRAINT stock_issue_requests_pkey TO stock_indents_pkey;
ALTER TABLE stock_indents RENAME CONSTRAINT chk_stock_issue_requests_status TO chk_stock_indents_status;
ALTER TABLE stock_indents RENAME CONSTRAINT chk_stock_issue_requests_different_locations TO chk_stock_indents_different_locations;
ALTER TABLE stock_indents RENAME CONSTRAINT stock_issue_requests_requesting_location_id_fkey TO stock_indents_requesting_location_id_fkey;
ALTER TABLE stock_indents RENAME CONSTRAINT stock_issue_requests_issuing_location_id_fkey TO stock_indents_issuing_location_id_fkey;

ALTER INDEX idx_stock_issue_requests_requesting RENAME TO idx_stock_indents_requesting;
ALTER INDEX idx_stock_issue_requests_issuing RENAME TO idx_stock_indents_issuing;
ALTER INDEX idx_stock_issue_requests_status RENAME TO idx_stock_indents_status;

ALTER TABLE stock_indent_items RENAME CONSTRAINT stock_issue_request_items_pkey TO stock_indent_items_pkey;
ALTER TABLE stock_indent_items RENAME CONSTRAINT uq_stock_issue_request_items_product TO uq_stock_indent_items_product;
ALTER TABLE stock_indent_items RENAME CONSTRAINT chk_stock_issue_request_items_requested_qty TO chk_stock_indent_items_requested_qty;
ALTER TABLE stock_indent_items RENAME CONSTRAINT chk_stock_issue_request_items_status TO chk_stock_indent_items_status;
ALTER TABLE stock_indent_items RENAME CONSTRAINT chk_stock_issue_request_items_returned_qty TO chk_stock_indent_items_returned_qty;
ALTER TABLE stock_indent_items RENAME CONSTRAINT stock_issue_request_items_stock_issue_request_id_fkey TO stock_indent_items_stock_indent_id_fkey;
ALTER TABLE stock_indent_items RENAME CONSTRAINT stock_issue_request_items_product_id_fkey TO stock_indent_items_product_id_fkey;
ALTER TABLE stock_indent_items RENAME CONSTRAINT stock_issue_request_items_variant_id_fkey TO stock_indent_items_variant_id_fkey;

ALTER INDEX idx_stock_issue_request_items_request RENAME TO idx_stock_indent_items_indent;
ALTER INDEX idx_stock_issue_request_items_product RENAME TO idx_stock_indent_items_product;
ALTER INDEX idx_stock_issue_request_items_variant RENAME TO idx_stock_indent_items_variant;
