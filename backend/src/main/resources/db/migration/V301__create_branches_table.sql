-- Campus Infrastructure hierarchy, level 1: belongs to exactly one Organization.

CREATE TABLE branches (
    id              BIGSERIAL     PRIMARY KEY,
    organization_id BIGINT        NOT NULL REFERENCES organizations(id),
    name            VARCHAR(100)  NOT NULL,
    code            VARCHAR(50)   NOT NULL,
    description     VARCHAR(500),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_branches_org_name UNIQUE (organization_id, name),
    CONSTRAINT uq_branches_org_code UNIQUE (organization_id, code)
);

CREATE INDEX idx_branches_organization_id ON branches(organization_id);
