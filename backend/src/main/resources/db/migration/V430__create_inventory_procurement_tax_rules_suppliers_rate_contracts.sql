-- Inventory Management (Release 3) — Phase 2 "Purchasing & Suppliers" first slice: TaxRule
-- (minimal master — a real pluggable tax engine per GAP-02/03 is deferred until there's an actual
-- second regime/accounting connector to design against), Supplier, and RateContract. Delivery
-- order and scope decisions (no PO approval gate this phase, minimal TaxRule now, rate-lookup-only
-- price comparison): docs/inventory-management/DECISION_LOG.md's 2026-09-08 "Phase 2 kickoff"
-- entry. Column names verified against V422/V426's own tables.

CREATE TABLE tax_rules (
    id           BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    rate_percent NUMERIC(5,2)  NOT NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tax_rules_name UNIQUE (name),
    CONSTRAINT chk_tax_rules_rate_percent CHECK (rate_percent >= 0)
);

-- bank_account_number and tax_registration_id/legal_registration_no are masked to their last 4
-- characters in SupplierResponse for a caller holding only SUPPLIER_VIEW (not SUPPLIER_MANAGE) —
-- same masking principle this module's decision log already committed to for sensitive
-- bank/PAN-equivalent references. portal_access_enabled is a reserved flag for a future actual
-- vendor-portal login capability — no portal/auth is built against it in this slice, same
-- "flag now, build later" precedent as Product.isLoanable before LoanableItemIssue existed.
CREATE TABLE suppliers (
    id                    BIGSERIAL     PRIMARY KEY,
    supplier_code         VARCHAR(50)   NOT NULL,
    supplier_name         VARCHAR(200)  NOT NULL,
    tax_registration_id   VARCHAR(50),
    legal_registration_no VARCHAR(50),
    bank_account_number   VARCHAR(40),
    bank_ifsc_code        VARCHAR(20),
    bank_name             VARCHAR(150),
    bank_account_holder   VARCHAR(150),
    contact_person        VARCHAR(150),
    email                 VARCHAR(150),
    phone                 VARCHAR(30),
    is_approved           BOOLEAN       NOT NULL DEFAULT FALSE,
    approval_date         TIMESTAMPTZ,
    portal_access_enabled BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_suppliers_code UNIQUE (supplier_code)
);

CREATE INDEX idx_suppliers_name ON suppliers(supplier_name);

CREATE TABLE rate_contracts (
    id                     BIGSERIAL     PRIMARY KEY,
    supplier_id            BIGINT        NOT NULL REFERENCES suppliers(id),
    start_date             DATE          NOT NULL,
    end_date               DATE,
    contract_value_cap     NUMERIC(14,2),
    terms_text             VARCHAR(2000),
    renewal_reminder_date  DATE,
    is_active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rate_contracts_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_rate_contracts_supplier ON rate_contracts(supplier_id);
