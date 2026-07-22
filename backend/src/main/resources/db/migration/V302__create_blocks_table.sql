-- Campus Infrastructure hierarchy, level 2: Organization > Branch > Block > Floor > Zone > Room.
-- Generic/shared physical structure — not hostel-specific — so Classroom/Lab can migrate their
-- free-text `building` field onto this hierarchy in a later, separate pass.
--
-- Name/code uniqueness is scoped per-branch (not global) — matches the rest of the hierarchy's
-- parent-scoped uniqueness pattern (Floor within Block, Zone within Floor, Room within Zone).
--
-- is_hostel + gender_restriction: an admin can mark ANY level from Block down to Zone as hostel
-- space and assign it a gender; setting it here cascades the same value down to every Floor and
-- Zone underneath (see CampusInfrastructureService). Not permanent — re-settable at any time, and
-- a child can be independently edited afterward to differ from its parent's last cascade. This is
-- a coarse organizational label, not itself billing-relevant — the binding, priced designation for
-- an individual room stays on hostel_rooms (V306).

CREATE TABLE blocks (
    id                  BIGSERIAL     PRIMARY KEY,
    branch_id           BIGINT        NOT NULL REFERENCES branches(id),
    name                VARCHAR(100)  NOT NULL,
    code                VARCHAR(50)   NOT NULL,
    description         VARCHAR(500),
    is_hostel           BOOLEAN       NOT NULL DEFAULT FALSE,
    gender_restriction  VARCHAR(20),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blocks_branch_name UNIQUE (branch_id, name),
    CONSTRAINT uq_blocks_branch_code UNIQUE (branch_id, code),
    CONSTRAINT chk_blocks_gender_restriction CHECK (gender_restriction IN ('BOYS', 'GIRLS') OR gender_restriction IS NULL)
);

CREATE INDEX idx_blocks_branch_id ON blocks(branch_id);
