-- Inventory Management — Phase 2 "Purchasing & Suppliers" stretch slice: Quotation Request
-- (RFQ). Sits between Purchase Requisition and Purchase Order as an OPTIONAL path — staff can
-- still create a Purchase Order directly from an APPROVED requisition item as before; raising a
-- Quotation Request first is not mandatory. One header invites 2+ suppliers to quote against a
-- set of APPROVED purchase_requisition_items; each (line, supplier) pair's quoted price is keyed
-- in by staff once received (no outbound email/portal exists anywhere in this module — same
-- staff-entry posture as the rest of Purchasing & Suppliers). Each line is awarded independently
-- (per-line, not one winner for the whole request), so awarding can spawn more than one Purchase
-- Order, one per winning supplier. No minimum-quote-count gate before award, matching this
-- module's generally unconstrained posture elsewhere. See docs/inventory-management/
-- DECISION_LOG.md's "Quotation Request slice" entry.
--
-- Status shape mirrors purchase_requisitions (V434)/purchase_orders (V438) exactly. Column names
-- verified against V426's inventory_locations, V424's products, V430's suppliers, and V434's
-- purchase_requisition_items — this worktree branched before main's V513-V515, so this migration
-- starts at V516 to avoid colliding with those on merge.

CREATE TABLE quotation_requests (
    id                 BIGSERIAL     PRIMARY KEY,
    location_id        BIGINT        NOT NULL REFERENCES inventory_locations(id),
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    request_date       DATE          NOT NULL,
    notes              VARCHAR(500),
    created_by         VARCHAR(255),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    submitted_by       VARCHAR(255),
    submitted_at       TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quotation_requests_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_quotation_requests_location ON quotation_requests(location_id);
CREATE INDEX idx_quotation_requests_status ON quotation_requests(status);

-- No variant column: like purchase_requisition_items, a variant is chosen fresh only once a
-- Purchase Order line is actually created, not carried through the requisition/quotation stages.
CREATE TABLE quotation_request_lines (
    id                            BIGSERIAL     PRIMARY KEY,
    quotation_request_id         BIGINT        NOT NULL REFERENCES quotation_requests(id) ON DELETE CASCADE,
    product_id                    BIGINT        NOT NULL REFERENCES products(id),
    purchase_requisition_item_id BIGINT        NOT NULL REFERENCES purchase_requisition_items(id),
    requested_qty                 NUMERIC(14,3) NOT NULL,
    status                        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    awarded_response_line_id      BIGINT,
    awarded_by                    VARCHAR(255),
    awarded_at                    TIMESTAMPTZ,
    CONSTRAINT uq_quotation_request_lines_requisition_item UNIQUE (purchase_requisition_item_id),
    CONSTRAINT chk_quotation_request_lines_requested_qty CHECK (requested_qty > 0),
    CONSTRAINT chk_quotation_request_lines_status CHECK (status IN ('PENDING', 'AWARDED', 'REJECTED', 'ORDERED'))
);

CREATE INDEX idx_quotation_request_lines_request ON quotation_request_lines(quotation_request_id);
CREATE INDEX idx_quotation_request_lines_product ON quotation_request_lines(product_id);

CREATE TABLE quotation_request_suppliers (
    id                    BIGSERIAL     PRIMARY KEY,
    quotation_request_id BIGINT        NOT NULL REFERENCES quotation_requests(id) ON DELETE CASCADE,
    supplier_id           BIGINT        NOT NULL REFERENCES suppliers(id),
    invited_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_quotation_request_suppliers UNIQUE (quotation_request_id, supplier_id)
);

CREATE INDEX idx_quotation_request_suppliers_request ON quotation_request_suppliers(quotation_request_id);

CREATE TABLE quotation_response_lines (
    id                          BIGSERIAL     PRIMARY KEY,
    quotation_request_line_id BIGINT        NOT NULL REFERENCES quotation_request_lines(id) ON DELETE CASCADE,
    supplier_id                 BIGINT        NOT NULL REFERENCES suppliers(id),
    quoted_unit_price            NUMERIC(14,2) NOT NULL,
    quoted_lead_time_days        INTEGER,
    notes                        VARCHAR(500),
    recorded_by                  VARCHAR(255),
    recorded_at                  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_quotation_response_lines UNIQUE (quotation_request_line_id, supplier_id),
    CONSTRAINT chk_quotation_response_lines_price CHECK (quoted_unit_price >= 0)
);

CREATE INDEX idx_quotation_response_lines_line ON quotation_response_lines(quotation_request_line_id);

ALTER TABLE quotation_request_lines
    ADD CONSTRAINT fk_quotation_request_lines_awarded_response
    FOREIGN KEY (awarded_response_line_id) REFERENCES quotation_response_lines(id);

-- Traceability from the eventual Purchase Order line back to the awarded quote, mirroring how
-- purchase_order_items already links back to purchase_requisition_item_id (V438).
ALTER TABLE purchase_order_items
    ADD COLUMN quotation_request_line_id BIGINT REFERENCES quotation_request_lines(id);

ALTER TABLE purchase_order_items
    ADD CONSTRAINT uq_purchase_order_items_quotation_request_line UNIQUE (quotation_request_line_id);
