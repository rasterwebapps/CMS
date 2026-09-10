-- Inventory Management — Procurement tax engine (GAP-02 pickup). Replaces the flat, single-rate
-- TaxRule with a real TaxType -> Tax -> TaxSubType hierarchy, modeled on OnePharmacy's own
-- taxType/tax/taxSubType design: a TaxType (GST) groups Tax rows (e.g. "GST 12%"), and each Tax
-- fans out into TaxSubType components (IGST 100% interstate; CGST/SGST split intrastate) that get
-- applied per PO line depending on whether the order is interstate or intrastate. See the
-- 2026-09-09 "GAP-02 pickup" decision-log entry for the specialist-round decisions this
-- implements. Column names verified against V430's tax_rules/suppliers and V438's
-- purchase_order_items/purchase_requisition_items — this is the same tax_rules table those
-- migrations created, extended in place rather than replaced (Tax = tax_rules, unchanged table
-- name, per the Backend Architect round's "reuse tax_rules as the Tax tier" decision).
--
-- The whole procurement/supplier chain is pre-production dev data at this point (confirmed with
-- the user before writing this migration, per CLAUDE.md's production-data-safety gate) — so this
-- migration truncates it outright instead of attempting a backward-compatible data migration.
-- tax_rules itself is NOT truncated (existing rows are backfilled into a default TaxType instead)
-- since GAP-02's Round 2 decision was "assume near-empty, backfill idempotently" for that table
-- specifically.

-- ---------------------------------------------------------------------------------------------
-- 1. TaxType — the regime a Tax belongs to (GST, VAT, Sales Tax, ...).
-- ---------------------------------------------------------------------------------------------
CREATE TABLE tax_types (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tax_types_name UNIQUE (name)
);

INSERT INTO tax_types (name, description)
VALUES ('GST', 'Goods and Services Tax (India)')
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------------------------------
-- 2. Tax (= the existing tax_rules table) — now belongs to a TaxType. Any pre-existing row
--    (there is no seeded data, but a dev/local environment may have user-created rows) is
--    backfilled onto the default GST TaxType before the column is made mandatory, so this step
--    is a safe no-op on an empty table and a correct backfill on a non-empty one.
-- ---------------------------------------------------------------------------------------------
ALTER TABLE tax_rules ADD COLUMN tax_type_id BIGINT;

UPDATE tax_rules
SET tax_type_id = (SELECT id FROM tax_types WHERE name = 'GST')
WHERE tax_type_id IS NULL;

ALTER TABLE tax_rules ALTER COLUMN tax_type_id SET NOT NULL;
ALTER TABLE tax_rules ADD CONSTRAINT fk_tax_rules_tax_type FOREIGN KEY (tax_type_id) REFERENCES tax_types(id);

CREATE INDEX idx_tax_rules_tax_type ON tax_rules(tax_type_id);

-- ---------------------------------------------------------------------------------------------
-- 3. TaxSubType — the components a Tax's rate fans out into for a given jurisdiction mode. The
--    sum of split_percent across all rows sharing (tax_rule_id, jurisdiction_mode) must equal
--    100 — enforced in TaxSubTypeService (a per-row CHECK can't express a cross-row sum), the
--    same "cross-row invariant enforced at the service layer" spirit already used elsewhere in
--    this module for non-DB-expressible rules.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE tax_sub_types (
    id                 BIGSERIAL     PRIMARY KEY,
    tax_rule_id        BIGINT        NOT NULL REFERENCES tax_rules(id),
    jurisdiction_mode  VARCHAR(20)   NOT NULL,
    component_name     VARCHAR(50)   NOT NULL,
    split_percent      NUMERIC(5,2)  NOT NULL,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tax_sub_types_jurisdiction_mode CHECK (jurisdiction_mode IN ('INTERSTATE', 'INTRASTATE')),
    CONSTRAINT chk_tax_sub_types_split_percent CHECK (split_percent > 0 AND split_percent <= 100),
    CONSTRAINT uq_tax_sub_types_component UNIQUE (tax_rule_id, jurisdiction_mode, component_name)
);

CREATE INDEX idx_tax_sub_types_tax_rule ON tax_sub_types(tax_rule_id);

-- ---------------------------------------------------------------------------------------------
-- 4. Jurisdiction settings — a single-row singleton (id pinned to 1) holding the institution's
--    own home state, compared against a supplier's state to auto-pick INTERSTATE vs INTRASTATE
--    on each PO line. Starts with zero rows (not one seeded with an empty value) — the app
--    surfaces "jurisdiction not configured" until an admin sets it, rather than a fake default.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE inventory_tax_jurisdiction_settings (
    id          BIGINT        PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    home_state  VARCHAR(100)  NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(255)
);

-- ---------------------------------------------------------------------------------------------
-- 5. Wipe the pre-production procurement/supplier chain so `state` can be added as mandatory
--    without a backward-compatibility migration. TRUNCATE ... CASCADE on suppliers also empties
--    rate_contracts, purchase_orders/purchase_order_items, vendor_product_mappings,
--    asset_service_contracts, and consignment_agreements/consignment_stock_lines, since all of
--    them FK to suppliers — confirmed acceptable with the user (all pre-production dev data)
--    before running this. Requisition lines that had been picked into a now-deleted PO line are
--    reset back to APPROVED so they're pickable again.
-- ---------------------------------------------------------------------------------------------
UPDATE purchase_requisition_items SET status = 'APPROVED' WHERE status = 'ORDERED';

TRUNCATE TABLE suppliers CASCADE;

ALTER TABLE suppliers ADD COLUMN state VARCHAR(100) NOT NULL;

-- ---------------------------------------------------------------------------------------------
-- 6. Purchase Order line: which jurisdiction mode was resolved for this line (drives which
--    TaxSubType components got applied), plus the per-component snapshot itself — needed because
--    GST filing reports IGST/CGST/SGST amounts separately, not as one lump tax_amount. Snapshotted
--    at line-creation time, same "amounts computed and stored once, not recomputed live" spirit
--    tax_amount/line_total already use (see PurchaseOrderItem's own Javadoc).
-- ---------------------------------------------------------------------------------------------
ALTER TABLE purchase_order_items ADD COLUMN jurisdiction_mode VARCHAR(20);
ALTER TABLE purchase_order_items ADD CONSTRAINT chk_purchase_order_items_jurisdiction_mode
    CHECK (jurisdiction_mode IS NULL OR jurisdiction_mode IN ('INTERSTATE', 'INTRASTATE'));

CREATE TABLE purchase_order_item_tax_components (
    id                      BIGSERIAL     PRIMARY KEY,
    purchase_order_item_id  BIGINT        NOT NULL REFERENCES purchase_order_items(id) ON DELETE CASCADE,
    tax_sub_type_id         BIGINT        REFERENCES tax_sub_types(id),
    component_name          VARCHAR(50)   NOT NULL,
    split_percent_applied   NUMERIC(5,2)  NOT NULL,
    component_amount        NUMERIC(14,2) NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_po_item_tax_components_item ON purchase_order_item_tax_components(purchase_order_item_id);

-- ---------------------------------------------------------------------------------------------
-- 7. Permissions. TaxType is a new browsable master, so it gets its own VIEW/MANAGE pair.
--    TaxSubType rides on the existing INVENTORY_TAX_RULE_MANAGE/_VIEW — it's edited as a Tax's
--    own sub-rows, not a standalone operation, per the Security Lead round's decision. Jurisdiction
--    settings (the home-state singleton) is its own distinct operation/screen, so it gets its own
--    pair too, per the operation-wise permission mapping rule.
-- ---------------------------------------------------------------------------------------------
INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_TAX_TYPE_VIEW',                 'View Tax Types',                 'MASTER', 'Tax Types',                CURRENT_TIMESTAMP),
    ('INVENTORY_TAX_TYPE_MANAGE',               'Manage Tax Types',               'MASTER', 'Tax Types',                CURRENT_TIMESTAMP),
    ('INVENTORY_TAX_JURISDICTION_SETTINGS_VIEW',   'View Tax Jurisdiction Settings',   'MASTER', 'Tax Jurisdiction Settings', CURRENT_TIMESTAMP),
    ('INVENTORY_TAX_JURISDICTION_SETTINGS_MANAGE', 'Manage Tax Jurisdiction Settings', 'MASTER', 'Tax Jurisdiction Settings', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_TAX_TYPE_VIEW', 'INVENTORY_TAX_TYPE_MANAGE',
                 'INVENTORY_TAX_JURISDICTION_SETTINGS_VIEW', 'INVENTORY_TAX_JURISDICTION_SETTINGS_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
