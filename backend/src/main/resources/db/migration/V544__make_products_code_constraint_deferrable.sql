-- Makes uq_products_code (V424) DEFERRABLE INITIALLY DEFERRED so a bulk code reassignment (the
-- "Regenerate Product Codes" admin action, ProductCodeGeneratorService.regenerateAllCodes) can
-- write every product's new code inside one transaction without a transient collision against a
-- not-yet-updated product's old code — Postgres only checks the constraint at COMMIT instead of
-- after each UPDATE. No behavior change for ordinary single-row create/update: the check still
-- runs before the transaction completes, so a real duplicate is still rejected, just at commit
-- time rather than statement time.

ALTER TABLE products DROP CONSTRAINT uq_products_code;
ALTER TABLE products ADD CONSTRAINT uq_products_code UNIQUE (product_code) DEFERRABLE INITIALLY DEFERRED;
