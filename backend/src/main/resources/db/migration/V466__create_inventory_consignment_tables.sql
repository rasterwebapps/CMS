-- Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") second slice — vendor-owned
-- ("consignment") stock. ConsignmentAgreement is the header (terms, mirrors the already-shipped
-- Budget master's shape); ConsignmentStockLine is the vendor-ownership side-ledger, one row per
-- (agreement, product), storing only receivedQty/consumedQty as raw facts — qtyOnHand is derived
-- live in the service, never stored. See ER_DIAGRAM_AND_MODULE_BOUNDARIES.md section 6 and the
-- "Consignment stock slice" decision-log entry.

CREATE TABLE consignment_agreements (
    id                   BIGSERIAL PRIMARY KEY,
    supplier_id          BIGINT       NOT NULL REFERENCES suppliers(id),
    location_id          BIGINT       NOT NULL REFERENCES inventory_locations(id),
    agreement_number     VARCHAR(100) NOT NULL,
    start_date           DATE         NOT NULL,
    end_date             DATE,
    billing_cycle_days   INTEGER,
    notes                VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consignment_agreements_supplier ON consignment_agreements(supplier_id);
CREATE INDEX idx_consignment_agreements_location ON consignment_agreements(location_id);

CREATE TABLE consignment_stock_lines (
    id                   BIGSERIAL PRIMARY KEY,
    agreement_id         BIGINT        NOT NULL REFERENCES consignment_agreements(id),
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    consignment_price    NUMERIC(14,2) NOT NULL,
    received_qty         NUMERIC(14,3) NOT NULL DEFAULT 0,
    consumed_qty         NUMERIC(14,3) NOT NULL DEFAULT 0,
    last_received_by     VARCHAR(255),
    last_received_at     TIMESTAMP,
    last_consumed_by     VARCHAR(255),
    last_consumed_at     TIMESTAMP,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_consignment_stock_line_agreement_product UNIQUE (agreement_id, product_id),
    CONSTRAINT chk_consignment_stock_line_consumed_not_over_received CHECK (consumed_qty <= received_qty)
);

CREATE INDEX idx_consignment_stock_lines_agreement ON consignment_stock_lines(agreement_id);
CREATE INDEX idx_consignment_stock_lines_product ON consignment_stock_lines(product_id);
