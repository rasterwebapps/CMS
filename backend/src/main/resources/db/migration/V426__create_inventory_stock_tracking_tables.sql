-- Inventory Management (Release 3) — Phase 1 "Core stock-tracking record" slice: InventoryLocation
-- (wraps an existing Infra Room), StockBatch, the append-only StockLedger, and the materialized
-- StockBalance kept in sync on every ledger write. Full design + this slice's deliberate
-- deviations from the ER draft (Room-only locations, narrowed movement types, weighted-average
-- decrease valuation): docs/inventory-management/DECISION_LOG.md's 2026-09-07 "Stock Tracking
-- slice" entry. Column names verified against V422/V424's own categories/products/uoms tables and
-- against rooms(id) (backend/src/main/java/com/cms/model/Room.java, table "rooms").

CREATE TABLE inventory_locations (
    id             BIGSERIAL     PRIMARY KEY,
    room_id        BIGINT        NOT NULL REFERENCES rooms(id),
    virtual_name   VARCHAR(150)  NOT NULL,
    location_role  VARCHAR(20)   NOT NULL,
    description    VARCHAR(500),
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_locations_virtual_name UNIQUE (virtual_name),
    CONSTRAINT chk_inventory_locations_role CHECK (location_role IN ('STORE', 'REQUESTING_POINT', 'BOTH'))
);

CREATE INDEX idx_inventory_locations_room ON inventory_locations(room_id);

CREATE TABLE stock_batches (
    id                  BIGSERIAL     PRIMARY KEY,
    product_id          BIGINT        NOT NULL REFERENCES products(id),
    batch_or_serial_no  VARCHAR(100)  NOT NULL,
    expiry_date         DATE,
    is_consignment      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_batches_product_batch UNIQUE (product_id, batch_or_serial_no)
);

CREATE INDEX idx_stock_batches_product ON stock_batches(product_id);

-- Append-only — a row is never updated or deleted once written. No updated_at/is_active for
-- exactly that reason.
CREATE TABLE stock_ledger (
    id            BIGSERIAL      PRIMARY KEY,
    product_id    BIGINT         NOT NULL REFERENCES products(id),
    location_id   BIGINT         NOT NULL REFERENCES inventory_locations(id),
    batch_id      BIGINT         REFERENCES stock_batches(id),
    txn_type      VARCHAR(30)    NOT NULL,
    qty_delta     NUMERIC(14,3)  NOT NULL,
    unit_cost     NUMERIC(14,2),
    ref_type      VARCHAR(50),
    ref_id        BIGINT,
    notes         VARCHAR(500),
    performed_by  VARCHAR(255),
    txn_date      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_ledger_txn_type CHECK (txn_type IN
        ('RECEIPT', 'ISSUE', 'TRANSFER', 'ADJUSTMENT', 'RETURN', 'CONSIGNMENT_CONSUMPTION', 'DISPOSAL'))
);

CREATE INDEX idx_stock_ledger_product_location ON stock_ledger(product_id, location_id);
CREATE INDEX idx_stock_ledger_batch ON stock_ledger(batch_id);
CREATE INDEX idx_stock_ledger_txn_date ON stock_ledger(txn_date);

-- Materialized/derived — kept in sync by application code on every stock_ledger write, never
-- written to directly by a user. NULLS NOT DISTINCT so a NULL batch_id still collapses to one row
-- per (product, location) instead of a fresh row per un-batched movement.
CREATE TABLE stock_balances (
    id             BIGSERIAL      PRIMARY KEY,
    product_id     BIGINT         NOT NULL REFERENCES products(id),
    location_id    BIGINT         NOT NULL REFERENCES inventory_locations(id),
    batch_id       BIGINT         REFERENCES stock_batches(id),
    qty_on_hand    NUMERIC(14,3)  NOT NULL DEFAULT 0,
    value_on_hand  NUMERIC(14,2)  NOT NULL DEFAULT 0,
    last_updated   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_balances UNIQUE NULLS NOT DISTINCT (product_id, location_id, batch_id)
);

CREATE INDEX idx_stock_balances_product ON stock_balances(product_id);
CREATE INDEX idx_stock_balances_location ON stock_balances(location_id);
