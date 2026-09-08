-- Inventory Management (Release 3) — Phase 6 "Budgets & Approvals" first slice: Budget
-- allocation. A plain date-range period (not coupled to this app's AcademicYear/term calendar —
-- this module stays industry-agnostic). Consumed amount is computed live from
-- purchase_order_items, never stored here. See docs/inventory-management/DECISION_LOG.md's
-- "Budget allocation slice" entry. Column names verified against V426's inventory_locations.

CREATE TABLE budgets (
    id                  BIGSERIAL     PRIMARY KEY,
    location_id         BIGINT        NOT NULL REFERENCES inventory_locations(id),
    period_start_date   DATE          NOT NULL,
    period_end_date     DATE          NOT NULL,
    allocated_amount    NUMERIC(14,2) NOT NULL,
    notes               VARCHAR(500),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_budgets_allocated_amount CHECK (allocated_amount >= 0),
    CONSTRAINT chk_budgets_period CHECK (period_end_date >= period_start_date)
);

CREATE INDEX idx_budgets_location ON budgets(location_id);
CREATE INDEX idx_budgets_period ON budgets(period_start_date, period_end_date);
