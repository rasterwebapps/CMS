package com.cms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.cms.model.enums.RoomPreferenceStatus;

public record RoomPreferenceRequest(
    Long enquiryId,

    Long studentId,

    @NotNull(message = "Preferred room type is required")
    Long preferredRoomTypeId,

    Long preferredZoneId,

    RoomPreferenceStatus status,

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    String remarks
) {}
