-- Inventory Management (Release 3) — Phase 5 "Equipment & Asset Management" second slice:
-- Maintenance Scheduling + Service Contracts. See docs/inventory-management/DECISION_LOG.md's
-- "Maintenance & Service Contracts slice" entry. Column names verified against V452's assets
-- and V430's suppliers.

CREATE TABLE asset_maintenance_schedules (
    id                        BIGSERIAL     PRIMARY KEY,
    asset_id                  BIGINT        NOT NULL REFERENCES assets(id),
    schedule_type             VARCHAR(20)   NOT NULL,
    recurrence_interval_days  INTEGER,
    next_due_date             DATE          NOT NULL,
    last_performed_date       DATE,
    is_active                 BOOLEAN       NOT NULL DEFAULT TRUE,
    notes                     VARCHAR(500),
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_asset_maintenance_schedules_type CHECK (schedule_type IN ('ONE_OFF', 'RECURRING'))
);

CREATE INDEX idx_asset_maintenance_schedules_asset ON asset_maintenance_schedules(asset_id);
CREATE INDEX idx_asset_maintenance_schedules_next_due ON asset_maintenance_schedules(next_due_date);

CREATE TABLE asset_service_contracts (
    id                     BIGSERIAL     PRIMARY KEY,
    asset_id               BIGINT        NOT NULL REFERENCES assets(id),
    supplier_id            BIGINT        NOT NULL REFERENCES suppliers(id),
    contract_number        VARCHAR(100),
    start_date             DATE          NOT NULL,
    end_date               DATE,
    renewal_reminder_date  DATE,
    coverage_details       VARCHAR(1000),
    is_active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_asset_service_contracts_asset ON asset_service_contracts(asset_id);
CREATE INDEX idx_asset_service_contracts_supplier ON asset_service_contracts(supplier_id);
