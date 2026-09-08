-- Inventory Management (Release 3) — Phase 4 "Requests, Issues & Returns" fourth and final
-- slice: Loanable Item Issue. A generic "borrow and return" record for a Product flagged
-- is_loanable — deliberately standalone, not linked into stock_ledger/stock_balances (the item
-- isn't consumed; see docs/inventory-management/DECISION_LOG.md's "Loanable Item Issue slice"
-- entry for why). No borrower entity exists in this generic module, so the borrower is captured
-- as plain text. Column names verified against V424's products and V426's inventory_locations.

CREATE TABLE loanable_item_issues (
    id                     BIGSERIAL     PRIMARY KEY,
    product_id             BIGINT        NOT NULL REFERENCES products(id),
    location_id            BIGINT        NOT NULL REFERENCES inventory_locations(id),
    borrower_name          VARCHAR(200)  NOT NULL,
    borrower_contact       VARCHAR(100),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'ISSUED',
    issue_date             DATE          NOT NULL,
    expected_return_date   DATE          NOT NULL,
    actual_return_date     DATE,
    condition_on_issue     VARCHAR(500),
    condition_on_return    VARCHAR(500),
    notes                  VARCHAR(500),
    issued_by              VARCHAR(255),
    issued_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    returned_by            VARCHAR(255),
    returned_at            TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_loanable_item_issues_status CHECK (status IN ('ISSUED', 'RETURNED'))
);

CREATE INDEX idx_loanable_item_issues_product ON loanable_item_issues(product_id);
CREATE INDEX idx_loanable_item_issues_location ON loanable_item_issues(location_id);
CREATE INDEX idx_loanable_item_issues_status ON loanable_item_issues(status);
CREATE INDEX idx_loanable_item_issues_expected_return ON loanable_item_issues(expected_return_date);
