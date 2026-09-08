-- Product photo upload — the ProductImage the "Product slice" decision-log entry deliberately
-- deferred, picked up per the plan's "Also outstanding" checklist item. Follows the FloorPlan
-- table's own shape (a storage_key reference into MinIO, never the binary itself). See
-- ER_DIAGRAM_AND_MODULE_BOUNDARIES.md section 2 and the "Product Image slice" decision-log entry.

CREATE TABLE product_images (
    id                     BIGSERIAL PRIMARY KEY,
    product_id             BIGINT       NOT NULL REFERENCES products(id),
    storage_key            VARCHAR(500) NOT NULL,
    original_file_name     VARCHAR(255),
    original_content_type  VARCHAR(100),
    is_primary             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_images_product ON product_images(product_id);
