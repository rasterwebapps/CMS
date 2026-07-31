package com.cms.dto;

import java.time.Instant;

public record RoomResponse(
    Long id,
    String roomNumber,
    Integer capacity,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long zoneId,
    String zoneName,
    /** Non-null when this room has been designated a hostel room (see HostelRoomResponse). */
    Long hostelRoomId,
    Long hostelRoomTypeId,
    String hostelRoomTypeName,

    Long purposeCategoryId,
    String purposeCategoryName,
    Long subTypeId,
    String subTypeName
) {}
