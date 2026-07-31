-- Room Purpose Classification (2-tier): every Room can be tagged with a Primary Purpose Category
-- (Academic, Residential, Administrative, etc.) and a Sub-Type belonging to that category
-- (Classroom, Student Bedroom, Staff Room, etc.), so other modules can query "rooms of purpose X"
-- instead of relying on the Block/Floor/Zone isHostel/genderRestriction cascade, which is purely
-- informational today and never reaches Room. Both tiers are full master-data tables (not
-- hardcoded enums) so admins can rename/extend them without a deploy.

CREATE TABLE room_purpose_categories (
    id             BIGSERIAL     PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    code           VARCHAR(50)   NOT NULL,
    is_residential BOOLEAN       NOT NULL DEFAULT FALSE,
    description    VARCHAR(500),
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_purpose_categories_name UNIQUE (name),
    CONSTRAINT uq_room_purpose_categories_code UNIQUE (code)
);

-- is_residential is a real column (not a magic `code = 'RESIDENTIAL'` string check in Java) so the
-- "does this category permit hostel-room assignment" gate survives an admin renaming the category.
CREATE INDEX idx_room_purpose_categories_is_residential ON room_purpose_categories(is_residential);

CREATE TABLE room_sub_types (
    id                  BIGSERIAL     PRIMARY KEY,
    purpose_category_id BIGINT        NOT NULL REFERENCES room_purpose_categories(id),
    name                VARCHAR(150)  NOT NULL,
    code                VARCHAR(50)   NOT NULL,
    description         VARCHAR(500),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_sub_types_name UNIQUE (purpose_category_id, name),
    CONSTRAINT uq_room_sub_types_code UNIQUE (purpose_category_id, code)
);

CREATE INDEX idx_room_sub_types_category ON room_sub_types(purpose_category_id);
