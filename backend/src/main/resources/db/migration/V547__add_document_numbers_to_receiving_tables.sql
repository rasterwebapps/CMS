-- Sequential document numbers for Goods Receipt and Return to Supplier — same rationale as V546.

ALTER TABLE goods_receipts ADD COLUMN receipt_number VARCHAR(50);
ALTER TABLE goods_receipts ADD CONSTRAINT uq_goods_receipts_receipt_number UNIQUE (receipt_number) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE supplier_returns ADD COLUMN return_number VARCHAR(50);
ALTER TABLE supplier_returns ADD CONSTRAINT uq_supplier_returns_return_number UNIQUE (return_number) DEFERRABLE INITIALLY DEFERRED;
