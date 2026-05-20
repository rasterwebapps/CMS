-- V154: Add fee_category column to payment_receipts and seed DOCUMENT_VERIFICATION_MANAGE permission.

-- 1. Add fee_category to the unified payment receipts table.
ALTER TABLE payment_receipts
    ADD COLUMN IF NOT EXISTS fee_category VARCHAR(30);

-- 2. Seed the document verification permission.
INSERT INTO permissions (code, display_name, category)
SELECT 'DOCUMENT_VERIFICATION_MANAGE', 'Verify Student Documents', 'ADMISSION'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'DOCUMENT_VERIFICATION_MANAGE');
