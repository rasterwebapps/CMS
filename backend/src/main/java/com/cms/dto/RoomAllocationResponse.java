package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.RoomAllocationStatus;

public record RoomAllocationResponse(
    Long id,
    Long studentId,
    String studentName,
    Long hostelRoomId,
    Long roomId,
    String roomNumber,
    Long zoneId,
    String zoneName,
    Long roomTypeId,
    String roomTypeName,
    LocalDate startDate,
    LocalDate endDate,
    RoomAllocationStatus status,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
