-- Makes Cycle Count bin-aware (follow-on to OC-229's Rack/Bin sub-locations). A count line can
-- now optionally be scoped to a specific InventoryBin instead of the whole location --
-- CycleCountService.addLine computes systemQtySnapshot from that bin's own
-- stock_bin_allocations total rather than the location-wide balance when a bin is given, and
-- CycleCountService.approveLine passes it through to StockMovementService.recordMovement's
-- existing binId parameter so an approved variance posts back against that same bin. NULL (the
-- default, and every pre-existing row) keeps counting the whole location exactly as before -- the
-- existing uq_cycle_count_lines_product constraint (cycle_count_id, product_id) is untouched, so a
-- product still appears at most once per count sheet, whole-location or bin-scoped.
-- Column names verified against V507's inventory_bins(id) and V428's cycle_count_lines(id).

ALTER TABLE cycle_count_lines
    ADD COLUMN bin_id BIGINT REFERENCES inventory_bins(id);

CREATE INDEX idx_cycle_count_lines_bin ON cycle_count_lines(bin_id);
