-- Inventory Management (Release 3) — Phase 2 "Purchasing & Suppliers" third slice: Purchase
-- Requisition. A department/location requests specific products be purchased, independent of any
-- reorder-level computation (that's the separate, not-yet-built "Wanted List" slice) — approved or
-- rejected per line, mirroring the Cycle Count workflow shape (V428) rather than IHMS's flatter
-- reorder-triggered StockRequirementItem, since a human-submitted request needing sign-off is a
-- distinct concept from an auto-computed shortage list. See docs/inventory-management/
-- DECISION_LOG.md's "Reference architecture pivot" and "Purchase Requisition slice" entries.
-- Column names verified against V426's inventory_locations and the existing products table.

CREATE TABLE purchase_requisitions (
    id                 BIGSERIAL     PRIMARY KEY,
    location_id        BIGINT        NOT NULL REFERENCES inventory_locations(id),
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    requisition_date   DATE          NOT NULL,
    notes              VARCHAR(500),
    created_by         VARCHAR(255),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    submitted_by       VARCHAR(255),
    submitted_at       TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_purchase_requisitions_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_purchase_requisitions_location ON purchase_requisitions(location_id);
CREATE INDEX idx_purchase_requisitions_status ON purchase_requisitions(status);

CREATE TABLE purchase_requisition_items (
    id                     BIGSERIAL     PRIMARY KEY,
    purchase_requisition_id BIGINT       NOT NULL REFERENCES purchase_requisitions(id) ON DELETE CASCADE,
    product_id             BIGINT        NOT NULL REFERENCES products(id),
    requested_qty          NUMERIC(14,3) NOT NULL,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    resolved_by            VARCHAR(255),
    resolved_at            TIMESTAMPTZ,
    resolution_notes       VARCHAR(500),
    notes                  VARCHAR(500),
    CONSTRAINT uq_purchase_requisition_items_product UNIQUE (purchase_requisition_id, product_id),
    CONSTRAINT chk_purchase_requisition_items_requested_qty CHECK (requested_qty > 0),
    CONSTRAINT chk_purchase_requisition_items_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_purchase_requisition_items_requisition ON purchase_requisition_items(purchase_requisition_id);
CREATE INDEX idx_purchase_requisition_items_product ON purchase_requisition_items(product_id);
