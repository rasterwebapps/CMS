-- Inventory Management (Release 3) — Phase 3 "Receiving & Stock Movement" first slice: Goods
-- Receipt. Two-step save (DRAFT) -> confirm (CONFIRMED, posts stock) against one purchase_order,
-- mirroring IHMS's own Purchase/PurchaseItem draft->confirm shape. See docs/inventory-management/
-- DECISION_LOG.md's "Goods Receipt slice" entry. Column names verified against V438's
-- purchase_orders/purchase_order_items and V424's products.

CREATE TABLE goods_receipts (
    id                 BIGSERIAL     PRIMARY KEY,
    purchase_order_id  BIGINT        NOT NULL REFERENCES purchase_orders(id),
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    receipt_date       DATE          NOT NULL,
    notes              VARCHAR(500),
    created_by         VARCHAR(255),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    confirmed_by       VARCHAR(255),
    confirmed_at       TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_goods_receipts_status CHECK (status IN ('DRAFT', 'CONFIRMED'))
);

CREATE INDEX idx_goods_receipts_purchase_order ON goods_receipts(purchase_order_id);
CREATE INDEX idx_goods_receipts_status ON goods_receipts(status);

CREATE TABLE goods_receipt_lines (
    id                      BIGSERIAL     PRIMARY KEY,
    goods_receipt_id        BIGINT        NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    purchase_order_item_id  BIGINT        NOT NULL REFERENCES purchase_order_items(id),
    received_qty            NUMERIC(14,3) NOT NULL,
    unit_cost               NUMERIC(14,2),
    batch_or_serial_no      VARCHAR(100),
    expiry_date             DATE,
    notes                   VARCHAR(500),
    CONSTRAINT chk_goods_receipt_lines_received_qty CHECK (received_qty > 0)
);

CREATE INDEX idx_goods_receipt_lines_receipt ON goods_receipt_lines(goods_receipt_id);
CREATE INDEX idx_goods_receipt_lines_po_item ON goods_receipt_lines(purchase_order_item_id);
