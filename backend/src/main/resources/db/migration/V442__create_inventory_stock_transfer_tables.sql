-- Inventory Management (Release 3) — Phase 3 "Receiving & Stock Movement" second slice: Stock
-- Transfer between two inventory_locations, giving the TRANSFER StockTxnType (reserved since
-- V426, unused until now) a real screen. DRAFT -> COMPLETED (posts a decrease-at-source /
-- increase-at-destination movement pair per line) or CANCELLED (from DRAFT only). See docs/
-- inventory-management/DECISION_LOG.md's "Stock Transfer slice" entry. Column names verified
-- against V426's inventory_locations and V424's products.

CREATE TABLE stock_transfers (
    id                        BIGSERIAL     PRIMARY KEY,
    source_location_id        BIGINT        NOT NULL REFERENCES inventory_locations(id),
    destination_location_id   BIGINT        NOT NULL REFERENCES inventory_locations(id),
    status                    VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    transfer_date             DATE          NOT NULL,
    notes                     VARCHAR(500),
    created_by                VARCHAR(255),
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_by              VARCHAR(255),
    completed_at              TIMESTAMPTZ,
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_transfers_status CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_stock_transfers_different_locations CHECK (source_location_id <> destination_location_id)
);

CREATE INDEX idx_stock_transfers_source ON stock_transfers(source_location_id);
CREATE INDEX idx_stock_transfers_destination ON stock_transfers(destination_location_id);
CREATE INDEX idx_stock_transfers_status ON stock_transfers(status);

CREATE TABLE stock_transfer_lines (
    id                  BIGSERIAL     PRIMARY KEY,
    stock_transfer_id   BIGINT        NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    product_id          BIGINT        NOT NULL REFERENCES products(id),
    quantity            NUMERIC(14,3) NOT NULL,
    notes               VARCHAR(500),
    CONSTRAINT uq_stock_transfer_lines_product UNIQUE (stock_transfer_id, product_id),
    CONSTRAINT chk_stock_transfer_lines_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_stock_transfer_lines_transfer ON stock_transfer_lines(stock_transfer_id);
CREATE INDEX idx_stock_transfer_lines_product ON stock_transfer_lines(product_id);
