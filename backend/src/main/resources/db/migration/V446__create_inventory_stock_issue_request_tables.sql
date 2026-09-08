-- Inventory Management (Release 3) — Phase 4 "Requests, Issues & Returns" first slice: Stock
-- Issue Request. A requesting inventory_location asks an issuing one for on-hand stock, approved
-- or rejected per line (mirrors purchase_requisitions/purchase_requisition_items from V434,
-- except approving here also posts a real ISSUE stock movement). Naming distinct from Purchase
-- Requisition (buying from a supplier) — see docs/inventory-management/DECISION_LOG.md's "Stock
-- Issue Request slice" entry. Column names verified against V426's inventory_locations and
-- V424's products.

CREATE TABLE stock_issue_requests (
    id                       BIGSERIAL     PRIMARY KEY,
    requesting_location_id    BIGINT        NOT NULL REFERENCES inventory_locations(id),
    issuing_location_id       BIGINT        NOT NULL REFERENCES inventory_locations(id),
    status                   VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    request_date             DATE          NOT NULL,
    notes                    VARCHAR(500),
    created_by               VARCHAR(255),
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    submitted_by             VARCHAR(255),
    submitted_at             TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_issue_requests_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_stock_issue_requests_different_locations CHECK (requesting_location_id <> issuing_location_id)
);

CREATE INDEX idx_stock_issue_requests_requesting ON stock_issue_requests(requesting_location_id);
CREATE INDEX idx_stock_issue_requests_issuing ON stock_issue_requests(issuing_location_id);
CREATE INDEX idx_stock_issue_requests_status ON stock_issue_requests(status);

CREATE TABLE stock_issue_request_items (
    id                       BIGSERIAL     PRIMARY KEY,
    stock_issue_request_id   BIGINT        NOT NULL REFERENCES stock_issue_requests(id) ON DELETE CASCADE,
    product_id               BIGINT        NOT NULL REFERENCES products(id),
    requested_qty            NUMERIC(14,3) NOT NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    resolved_by              VARCHAR(255),
    resolved_at              TIMESTAMPTZ,
    resolution_notes         VARCHAR(500),
    notes                    VARCHAR(500),
    CONSTRAINT uq_stock_issue_request_items_product UNIQUE (stock_issue_request_id, product_id),
    CONSTRAINT chk_stock_issue_request_items_requested_qty CHECK (requested_qty > 0),
    CONSTRAINT chk_stock_issue_request_items_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_stock_issue_request_items_request ON stock_issue_request_items(stock_issue_request_id);
CREATE INDEX idx_stock_issue_request_items_product ON stock_issue_request_items(product_id);
