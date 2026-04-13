CREATE TABLE equipment (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)                NOT NULL,
    asset_code       VARCHAR(255)                UNIQUE,
    serial_number    VARCHAR(255),
    category         VARCHAR(255)                NOT NULL,
    lab_id           BIGINT                      NOT NULL REFERENCES labs(id),
    manufacturer     VARCHAR(255),
    model            VARCHAR(255),
    status           VARCHAR(255)                NOT NULL,
    purchase_date    DATE,
    purchase_price   NUMERIC(10,2),
    warranty_expiry  DATE,
    location         VARCHAR(255),
    specifications   VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
