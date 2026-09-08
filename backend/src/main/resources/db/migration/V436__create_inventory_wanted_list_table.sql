-- Inventory Management (Release 3) — Phase 2 "Purchasing & Suppliers" fourth slice: Wanted List.
-- The ERP-standard MRP "Planned Order" step ahead of Purchase Requisition: a scheduled job (plus
-- a manual "Run Now" trigger) nets each product's configured reorder level against current stock
-- and whatever's already open on a Purchase Requisition, so an already-requested shortfall is
-- never re-flagged. A line's only forward action is converting it (alone or together with other
-- lines for the same location) into a Purchase Requisition. See docs/inventory-management/
-- DECISION_LOG.md's "Wanted List slice" entry.
-- Column names verified against V424's products, V426's inventory_locations, and V434's
-- purchase_requisitions/purchase_requisition_items tables.

CREATE TABLE wanted_list_items (
    id                                      BIGSERIAL     PRIMARY KEY,
    product_id                              BIGINT        NOT NULL REFERENCES products(id),
    location_id                             BIGINT        NOT NULL REFERENCES inventory_locations(id),
    status                                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    qty_on_hand_snapshot                    NUMERIC(14,3) NOT NULL,
    qty_on_order_snapshot                   NUMERIC(14,3) NOT NULL DEFAULT 0,
    reorder_level_snapshot                  NUMERIC(14,3) NOT NULL,
    suggested_qty                           NUMERIC(14,3) NOT NULL,
    generated_at                            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    resolved_by                             VARCHAR(255),
    resolved_at                             TIMESTAMPTZ,
    resolution_notes                        VARCHAR(500),
    rejection_reason                        VARCHAR(30),
    converted_purchase_requisition_id       BIGINT        REFERENCES purchase_requisitions(id),
    converted_purchase_requisition_item_id  BIGINT        REFERENCES purchase_requisition_items(id),
    updated_at                              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_wanted_list_items_status CHECK (status IN ('PENDING', 'DEFERRED', 'REJECTED', 'CONVERTED')),
    CONSTRAINT chk_wanted_list_items_rejection_reason CHECK (
        rejection_reason IS NULL OR rejection_reason IN ('ALREADY_ORDERED_ELSEWHERE', 'PRODUCT_DISCONTINUING', 'LEVEL_MISCALIBRATED', 'OTHER')
    ),
    CONSTRAINT chk_wanted_list_items_suggested_qty CHECK (suggested_qty > 0)
);

-- At most one unresolved (PENDING/DEFERRED) line per (product, location) — the shortage job relies
-- on this to avoid duplicate spam every run; enforced here too, not just in the service layer.
CREATE UNIQUE INDEX uq_wanted_list_items_unresolved_product_location
    ON wanted_list_items(product_id, location_id)
    WHERE status IN ('PENDING', 'DEFERRED');

CREATE INDEX idx_wanted_list_items_location ON wanted_list_items(location_id);
CREATE INDEX idx_wanted_list_items_status ON wanted_list_items(status);
CREATE INDEX idx_wanted_list_items_product ON wanted_list_items(product_id);
