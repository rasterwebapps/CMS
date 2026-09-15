-- Inventory Rack -> Bin sub-locations within an InventoryLocation. Strict 3-level hierarchy:
-- InventoryLocation -> InventoryRack -> InventoryBin. Structurally mirrors Library's
-- library_racks/library_shelves (V252/V253) — uniqueness scoped per-parent, not global, since
-- two Locations may each have a "Rack A", and two Racks may each have a "Bin 1". Column names
-- verified against V426's inventory_locations(id) (backend/src/main/java/com/cms/inventory/stock/model/InventoryLocation.java).
-- These are organizational/locator sub-locations only in this pass — StockBalance/StockLedger
-- stay keyed on (product, variant, location, batch) as the source of truth; the splittable
-- per-bin stock breakdown reconciling to that balance is a separate, later migration (Phase 2).

CREATE TABLE inventory_racks (
    id           BIGSERIAL     PRIMARY KEY,
    location_id  BIGINT        NOT NULL REFERENCES inventory_locations(id),
    name         VARCHAR(200)  NOT NULL,
    code         VARCHAR(50)   NOT NULL,
    description  VARCHAR(500),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_racks_name UNIQUE (location_id, name),
    CONSTRAINT uq_inventory_racks_code UNIQUE (location_id, code)
);

CREATE INDEX idx_inventory_racks_location ON inventory_racks(location_id);

CREATE TABLE inventory_bins (
    id           BIGSERIAL     PRIMARY KEY,
    rack_id      BIGINT        NOT NULL REFERENCES inventory_racks(id),
    name         VARCHAR(200)  NOT NULL,
    code         VARCHAR(50)   NOT NULL,
    description  VARCHAR(500),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_bins_name UNIQUE (rack_id, name),
    CONSTRAINT uq_inventory_bins_code UNIQUE (rack_id, code)
);

CREATE INDEX idx_inventory_bins_rack ON inventory_bins(rack_id);
