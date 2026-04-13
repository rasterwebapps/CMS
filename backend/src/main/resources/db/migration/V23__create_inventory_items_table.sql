CREATE TABLE inventory_items (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)                NOT NULL,
    item_code        VARCHAR(255)                UNIQUE,
    lab_id           BIGINT                      NOT NULL REFERENCES labs(id),
    quantity         INTEGER                     NOT NULL,
    minimum_quantity INTEGER,
    unit             VARCHAR(255),
    description      VARCHAR(255),
    last_restocked   DATE,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
