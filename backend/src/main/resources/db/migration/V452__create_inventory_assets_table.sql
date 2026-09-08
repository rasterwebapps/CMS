-- Inventory Management (Release 3) — Phase 5 "Equipment & Asset Management" first slice: Asset
-- register. One physical, individually-tracked unit of a product (asset_tag is its unique
-- identity), distinct from the catalog/stock-ledger's aggregate quantity tracking. Status
-- lifecycle is open-ended (no strict state machine) — see docs/inventory-management/
-- DECISION_LOG.md's "Asset register slice" entry. Column names verified against V424's products,
-- V426's inventory_locations, and V440's goods_receipt_lines.

CREATE TABLE assets (
    id                    BIGSERIAL     PRIMARY KEY,
    product_id            BIGINT        NOT NULL REFERENCES products(id),
    location_id           BIGINT        NOT NULL REFERENCES inventory_locations(id),
    asset_tag             VARCHAR(50)   NOT NULL,
    serial_number         VARCHAR(100),
    status                VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    goods_receipt_line_id BIGINT        REFERENCES goods_receipt_lines(id),
    purchase_value        NUMERIC(14,2),
    purchase_date         DATE,
    useful_life_months    INTEGER,
    salvage_value         NUMERIC(14,2),
    notes                 VARCHAR(500),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_assets_asset_tag UNIQUE (asset_tag),
    CONSTRAINT chk_assets_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'UNDER_MAINTENANCE', 'RETIRED', 'DISPOSED'))
);

CREATE INDEX idx_assets_product ON assets(product_id);
CREATE INDEX idx_assets_location ON assets(location_id);
CREATE INDEX idx_assets_status ON assets(status);
