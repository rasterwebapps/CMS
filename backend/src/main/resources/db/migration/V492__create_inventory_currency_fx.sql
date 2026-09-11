-- Inventory Management — Phase 3 kickoff item #9 ("Multi-currency FX"): a configurable base
-- currency (singleton settings row, same shape as inventory_tax_jurisdiction_settings) and a
-- manually-maintained exchange-rate reference table, applied to VendorProductMapping's read model
-- only — PurchaseOrder.currencyCode/exchangeRate stay exactly as they are (plain, manually-entered
-- fields with "no conversion/revaluation engine", per that entity's own javadoc and the "Purchase
-- Order slice" decision-log entry; this slice doesn't reopen that decision). See
-- docs/inventory-management/DECISION_LOG.md.

CREATE TABLE inventory_currency_settings (
    id                  BIGINT      PRIMARY KEY,
    base_currency_code  VARCHAR(3)  NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_inventory_currency_settings_singleton CHECK (id = 1)
);

-- One row per (currency, effective date) — the rate that currency converts to the base currency
-- as of that date. Multiple dated rows per currency are allowed (rates change over time); the
-- most recent row with effective_date <= today is the one in effect, resolved at read time.
CREATE TABLE currency_exchange_rates (
    id              BIGSERIAL     PRIMARY KEY,
    currency_code   VARCHAR(3)    NOT NULL,
    rate_to_base    NUMERIC(18,6) NOT NULL,
    effective_date  DATE          NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_currency_exchange_rates_code_date UNIQUE (currency_code, effective_date),
    CONSTRAINT chk_currency_exchange_rates_rate_positive CHECK (rate_to_base > 0)
);

CREATE INDEX idx_currency_exchange_rates_code_date ON currency_exchange_rates(currency_code, effective_date DESC);
