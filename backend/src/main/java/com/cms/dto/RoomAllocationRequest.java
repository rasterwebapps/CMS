package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.cms.model.enums.RoomAllocationStatus;

public record RoomAllocationRequest(
    @NotNull(message = "Student is required")
    Long studentId,

    @NotNull(message = "Hostel room is required")
    Long hostelRoomId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    RoomAllocationStatus status,

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    String remarks
) {}
