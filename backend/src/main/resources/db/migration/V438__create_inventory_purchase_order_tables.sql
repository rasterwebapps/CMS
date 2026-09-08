-- Inventory Management (Release 3) — Phase 2 "Purchasing & Suppliers" fifth and final slice:
-- Purchase Order. Lines are picked up from APPROVED purchase_requisition_items for the chosen
-- supplier's location — the requisition line moves to a new terminal ORDERED status once picked,
-- so it can't be double-booked (widens the existing chk_purchase_requisition_items_status check
-- from V434 rather than editing that shipped migration). Status lifecycle mirrors IHMS's own
-- receipt-progress-driven PO states (PENDING/ORDERED/IN_PROGRESS/PARTIALLY_COMPLETED/COMPLETED/
-- FORCE_CLOSED) — see docs/inventory-management/DECISION_LOG.md's "Purchase Order slice" entry.
-- Column names verified against V424's products, V430's suppliers/tax_rules, V426's
-- inventory_locations, and V434's purchase_requisition_items.

ALTER TABLE purchase_requisition_items DROP CONSTRAINT chk_purchase_requisition_items_status;
ALTER TABLE purchase_requisition_items ADD CONSTRAINT chk_purchase_requisition_items_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ORDERED'));

CREATE TABLE purchase_orders (
    id                      BIGSERIAL     PRIMARY KEY,
    supplier_id             BIGINT        NOT NULL REFERENCES suppliers(id),
    location_id             BIGINT        NOT NULL REFERENCES inventory_locations(id),
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    po_date                 DATE          NOT NULL,
    expected_delivery_date  DATE,
    currency_code           VARCHAR(10)   NOT NULL DEFAULT 'INR',
    exchange_rate           NUMERIC(14,6),
    notes                   VARCHAR(500),
    created_by              VARCHAR(255),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    ordered_by              VARCHAR(255),
    ordered_at              TIMESTAMPTZ,
    force_closed_by         VARCHAR(255),
    force_closed_at         TIMESTAMPTZ,
    force_close_reason      VARCHAR(500),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_purchase_orders_status CHECK (
        status IN ('PENDING', 'ORDERED', 'IN_PROGRESS', 'PARTIALLY_COMPLETED', 'COMPLETED', 'FORCE_CLOSED')
    )
);

CREATE INDEX idx_purchase_orders_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_purchase_orders_location ON purchase_orders(location_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);

CREATE TABLE purchase_order_items (
    id                          BIGSERIAL     PRIMARY KEY,
    purchase_order_id           BIGINT        NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id                  BIGINT        NOT NULL REFERENCES products(id),
    purchase_requisition_item_id BIGINT       REFERENCES purchase_requisition_items(id),
    ordered_qty                 NUMERIC(14,3) NOT NULL,
    unit_price                  NUMERIC(14,2) NOT NULL,
    tax_rule_id                 BIGINT        REFERENCES tax_rules(id),
    tax_amount                  NUMERIC(14,2) NOT NULL DEFAULT 0,
    line_total                  NUMERIC(14,2) NOT NULL,
    received_qty                NUMERIC(14,3) NOT NULL DEFAULT 0,
    CONSTRAINT chk_purchase_order_items_ordered_qty CHECK (ordered_qty > 0),
    CONSTRAINT chk_purchase_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT uq_purchase_order_items_requisition_item UNIQUE (purchase_requisition_item_id)
);

CREATE INDEX idx_purchase_order_items_order ON purchase_order_items(purchase_order_id);
CREATE INDEX idx_purchase_order_items_product ON purchase_order_items(product_id);
