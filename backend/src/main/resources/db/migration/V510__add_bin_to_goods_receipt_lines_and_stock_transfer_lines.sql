-- Phase 2 of OC-229: optional bin selection on the two line types that can post a bin allocation
-- (Goods Receipt receives into one bin per line; Stock Transfer moves between a source and a
-- destination bin per line). All nullable — a line with no bin picked posts its stock movement
-- exactly as before (unbinned, breakdown table untouched for that leg). Column names verified
-- against goods_receipt_lines (V440) and stock_transfer_lines (V442) and inventory_bins (V507).

ALTER TABLE goods_receipt_lines ADD COLUMN bin_id BIGINT REFERENCES inventory_bins(id);

ALTER TABLE stock_transfer_lines ADD COLUMN source_bin_id BIGINT REFERENCES inventory_bins(id);
ALTER TABLE stock_transfer_lines ADD COLUMN destination_bin_id BIGINT REFERENCES inventory_bins(id);
