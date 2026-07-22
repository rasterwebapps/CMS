-- R2-4.1.4: binding, capacity-consuming room assignment. Creation is rejected in the service
-- layer (not here) unless the student's studentType is HOSTELER -- see RoomAllocationService.
-- Occupancy is checked against hostel_room_types.sharing_capacity by counting ACTIVE rows per
-- hostel_room_id; not enforced as a DB constraint since capacity is a joined, mutable value.

CREATE TABLE room_allocations (
    id             BIGSERIAL     PRIMARY KEY,
    student_id     BIGINT        NOT NULL REFERENCES students(id),
    hostel_room_id BIGINT        NOT NULL REFERENCES hostel_rooms(id),
    start_date     DATE          NOT NULL,
    end_date       DATE,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    remarks        VARCHAR(500),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_room_allocations_student_id ON room_allocations(student_id);
CREATE INDEX idx_room_allocations_hostel_room_id ON room_allocations(hostel_room_id);
CREATE INDEX idx_room_allocations_status ON room_allocations(status);
