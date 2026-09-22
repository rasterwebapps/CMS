-- Sequential document numbers for Quotation Request and Purchase Order, generated via the
-- existing (previously unused) NumberSeriesDefinition/ApplicationNumberSequenceService engine
-- (see V244). Nullable: existing rows predate this feature and get theirs via the "Regenerate
-- Numbers" admin action, not this migration. DEFERRABLE so that bulk regenerate action can write
-- every row's new number inside one transaction without a transient collision against a
-- not-yet-updated row's old number — Postgres checks uniqueness once, at commit (see V544's
-- identical rationale for products.product_code). Plain UNIQUE (not a partial index) is fine here
-- since Postgres already treats every NULL as distinct.

ALTER TABLE purchase_orders ADD COLUMN po_number VARCHAR(50);
ALTER TABLE purchase_orders ADD CONSTRAINT uq_purchase_orders_po_number UNIQUE (po_number) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE quotation_requests ADD COLUMN quotation_number VARCHAR(50);
ALTER TABLE quotation_requests ADD CONSTRAINT uq_quotation_requests_quotation_number UNIQUE (quotation_number) DEFERRABLE INITIALLY DEFERRED;
