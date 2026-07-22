-- Campus Infrastructure hierarchy, level 0 (new root):
-- Organization > Branch > Block > Floor > Zone > Room.
-- Added after Block/Floor/Zone/Room/HostelRoom (V302-V307 below) already shipped in this same
-- pre-release pass, hence the renumbering — Organization/Branch must exist before Block can FK to
-- Branch. Not hostel-specific: unlike Block/Floor/Zone, Organization and Branch deliberately carry
-- no isHostel/genderRestriction — that cascade starts at Block, since a whole Branch (e.g. an
-- entire campus) is never meaningfully single-gender the way a Block/Floor/Zone can be.

CREATE TABLE organizations (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    code        VARCHAR(50)   NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_organizations_name UNIQUE (name),
    CONSTRAINT uq_organizations_code UNIQUE (code)
);
