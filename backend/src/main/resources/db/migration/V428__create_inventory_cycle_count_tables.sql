-- Inventory Management (Release 3) — Phase 1 "Physical stock counts / reconciliation" slice.
-- Design + decisions (full-location + ad-hoc scope, blind count, separate approve permission with
-- auto-post against the unbatched balance only): docs/inventory-management/DECISION_LOG.md's
-- 2026-09-08 "Cycle Count slice" entry. Column names verified against V426's inventory_locations,
-- products, and stock_ledger tables.

CREATE TABLE cycle_counts (
    id             BIGSERIAL     PRIMARY KEY,
    location_id    BIGINT        NOT NULL REFERENCES inventory_locations(id),
    scope          VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    count_date     DATE          NOT NULL,
    notes          VARCHAR(500),
    created_by     VARCHAR(255),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    submitted_by   VARCHAR(255),
    submitted_at   TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cycle_counts_scope  CHECK (scope IN ('FULL_LOCATION', 'AD_HOC')),
    CONSTRAINT chk_cycle_counts_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_cycle_counts_location ON cycle_counts(location_id);
CREATE INDEX idx_cycle_counts_status ON cycle_counts(status);

-- system_qty_snapshot is taken once, when the line is added to the sheet (at count creation for a
-- FULL_LOCATION line, or at the moment it's added ad-hoc) — a moving target during the count
-- window would defeat the point of comparing against a fixed baseline. It's the sum across every
-- StockBalance row (batched and unbatched) for that product/location, matching what a person
-- physically counting the shelf would actually see; posting the resulting variance back to the
-- ledger, however, only ever touches the unbatched balance row (see CycleCountService) — a
-- deliberate limitation, same spirit as this module's other "no per-batch handling yet" calls.
CREATE TABLE cycle_count_lines (
    id                    BIGSERIAL     PRIMARY KEY,
    cycle_count_id        BIGINT        NOT NULL REFERENCES cycle_counts(id) ON DELETE CASCADE,
    product_id            BIGINT        NOT NULL REFERENCES products(id),
    system_qty_snapshot   NUMERIC(14,3) NOT NULL,
    counted_qty           NUMERIC(14,3),
    variance_qty          NUMERIC(14,3),
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING_COUNT',
    counted_by            VARCHAR(255),
    counted_at            TIMESTAMPTZ,
    resolved_by           VARCHAR(255),
    resolved_at           TIMESTAMPTZ,
    resolution_notes      VARCHAR(500),
    ledger_ref_id         BIGINT REFERENCES stock_ledger(id),
    notes                 VARCHAR(500),
    CONSTRAINT uq_cycle_count_lines_product UNIQUE (cycle_count_id, product_id),
    CONSTRAINT chk_cycle_count_lines_status CHECK (status IN
        ('PENDING_COUNT', 'MATCHED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_cycle_count_lines_count ON cycle_count_lines(cycle_count_id);
CREATE INDEX idx_cycle_count_lines_product ON cycle_count_lines(product_id);
