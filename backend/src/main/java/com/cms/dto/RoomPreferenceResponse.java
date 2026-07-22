package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.RoomPreferenceStatus;

public record RoomPreferenceResponse(
    Long id,
    Long enquiryId,
    String enquiryName,
    Long studentId,
    String studentName,
    Long preferredRoomTypeId,
    String preferredRoomTypeName,
    Long preferredZoneId,
    String preferredZoneName,
    RoomPreferenceStatus status,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
