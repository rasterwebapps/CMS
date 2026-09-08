-- Inventory Management (Release 3) — Phase 2 "Purchasing & Suppliers" second slice: VendorProductMapping
-- (a supplier's rate for a product, for rate-lookup price comparison — decision log decision #4)
-- and RateContractLine (per-product negotiated rates on an existing RateContract, added so a
-- mapping's RateContract link has an actual rate to override with). Specialist round + decisions:
-- docs/inventory-management/DECISION_LOG.md's "VendorProductMapping slice" entry (added alongside
-- this migration). Column names verified against V430's suppliers/rate_contracts and the existing
-- products/uoms tables.

CREATE TABLE vendor_product_mappings (
    id               BIGSERIAL     PRIMARY KEY,
    supplier_id      BIGINT        NOT NULL REFERENCES suppliers(id),
    product_id       BIGINT        NOT NULL REFERENCES products(id),
    rate_contract_id BIGINT        REFERENCES rate_contracts(id),
    unit_price       NUMERIC(14,2) NOT NULL,
    currency_code    VARCHAR(3)    NOT NULL DEFAULT 'INR',
    uom_id           BIGINT        REFERENCES uoms(id),
    min_order_qty    NUMERIC(14,3),
    lead_time_days   INTEGER,
    is_preferred     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_vendor_product_mappings_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_vendor_product_mappings_min_order_qty CHECK (min_order_qty IS NULL OR min_order_qty >= 0),
    CONSTRAINT chk_vendor_product_mappings_lead_time_days CHECK (lead_time_days IS NULL OR lead_time_days >= 0)
);

-- One active mapping per (supplier, product) pair; a deactivated mapping doesn't block a new one.
CREATE UNIQUE INDEX uq_vendor_product_mappings_active_pair
    ON vendor_product_mappings (supplier_id, product_id)
    WHERE is_active;

CREATE INDEX idx_vendor_product_mappings_product ON vendor_product_mappings(product_id);

-- Per-product negotiated rate lines on a RateContract. Managed as a child collection of RateContract
-- (replaced wholesale on save, no dedicated CRUD screen), same pattern as ProductAlias on Product.
CREATE TABLE rate_contract_lines (
    id                BIGSERIAL     PRIMARY KEY,
    rate_contract_id  BIGINT        NOT NULL REFERENCES rate_contracts(id) ON DELETE CASCADE,
    product_id        BIGINT        NOT NULL REFERENCES products(id),
    negotiated_rate   NUMERIC(14,2) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rate_contract_lines_contract_product UNIQUE (rate_contract_id, product_id),
    CONSTRAINT chk_rate_contract_lines_negotiated_rate CHECK (negotiated_rate >= 0)
);
