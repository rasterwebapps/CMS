-- Campus Infrastructure hierarchy, level 4: belongs to exactly one Zone. Generic physical room —
-- capacity here is informational/general-purpose (kept for future Classroom/Lab reuse); for rooms
-- used as hostel rooms, HostelRoomType.sharing_capacity (V299) is the authoritative occupancy
-- figure, not this column (see V304).

CREATE TABLE rooms (
    id          BIGSERIAL     PRIMARY KEY,
    zone_id     BIGINT        NOT NULL REFERENCES zones(id),
    room_number VARCHAR(50)   NOT NULL,
    capacity    INTEGER,
    description VARCHAR(500),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rooms_zone_room_number UNIQUE (zone_id, room_number),
    CONSTRAINT chk_rooms_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE INDEX idx_rooms_zone_id ON rooms(zone_id);
