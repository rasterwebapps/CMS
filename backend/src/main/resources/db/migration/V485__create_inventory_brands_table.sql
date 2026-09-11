-- Inventory Management — Phase 1 kickoff item #4 ("Brand/Manufacturer master + FK on Product"):
-- a new flat master (no hierarchy, same shape as UOM minus its code column — a brand name is its
-- own unique identifier, there's no standard "brand code" convention the way there is for units).

CREATE TABLE brands (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_brands_name UNIQUE (name)
);

-- Nullable: a product's brand/manufacturer is optional (e.g. a generic lab consumable has none).
ALTER TABLE products
    ADD COLUMN brand_id BIGINT REFERENCES brands(id);

CREATE INDEX idx_products_brand ON products(brand_id);
