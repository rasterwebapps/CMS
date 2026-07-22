-- Thin hostel-specific attachment onto a generic Room (V305): a Room becomes "a hostel room" by
-- having a row here, pairing it with a pricing/sharing category from hostel_room_types (V298).
-- room_id is unique — a physical room is at most one hostel room, never split across types.
-- Location (block/floor/zone) comes from the Room's zone chain; pricing/sharing/AC comes from
-- HostelRoomType; this table is purely the join between the two.

CREATE TABLE hostel_rooms (
    id           BIGSERIAL   PRIMARY KEY,
    room_id      BIGINT      NOT NULL UNIQUE REFERENCES rooms(id),
    room_type_id BIGINT      NOT NULL REFERENCES hostel_room_types(id),
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hostel_rooms_room_type_id ON hostel_rooms(room_type_id);
