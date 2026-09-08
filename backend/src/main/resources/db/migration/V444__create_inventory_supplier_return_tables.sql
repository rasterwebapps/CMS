-- Inventory Management (Release 3) — Phase 3 "Receiving & Stock Movement" third and final slice:
-- Return to Supplier, raised against a CONFIRMED goods_receipts row. See docs/inventory-
-- management/DECISION_LOG.md's "Return to Supplier slice" entry. Column names verified against
-- V440's goods_receipts/goods_receipt_lines and V424's products.

CREATE TABLE supplier_returns (
    id                BIGSERIAL     PRIMARY KEY,
    goods_receipt_id  BIGINT        NOT NULL REFERENCES goods_receipts(id),
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    reason            VARCHAR(30),
    return_date       DATE          NOT NULL,
    notes             VARCHAR(500),
    created_by        VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_by      VARCHAR(255),
    completed_at      TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_supplier_returns_status CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_supplier_returns_reason CHECK (reason IS NULL OR reason IN ('DEFECTIVE', 'WRONG_ITEM', 'DAMAGED_IN_TRANSIT', 'QUALITY_ISSUE', 'OTHER'))
);

CREATE INDEX idx_supplier_returns_receipt ON supplier_returns(goods_receipt_id);
CREATE INDEX idx_supplier_returns_status ON supplier_returns(status);

CREATE TABLE supplier_return_lines (
    id                     BIGSERIAL     PRIMARY KEY,
    supplier_return_id     BIGINT        NOT NULL REFERENCES supplier_returns(id) ON DELETE CASCADE,
    goods_receipt_line_id  BIGINT        NOT NULL REFERENCES goods_receipt_lines(id),
    returned_qty           NUMERIC(14,3) NOT NULL,
    notes                  VARCHAR(500),
    CONSTRAINT chk_supplier_return_lines_returned_qty CHECK (returned_qty > 0)
);

CREATE INDEX idx_supplier_return_lines_return ON supplier_return_lines(supplier_return_id);
CREATE INDEX idx_supplier_return_lines_receipt_line ON supplier_return_lines(goods_receipt_line_id);
