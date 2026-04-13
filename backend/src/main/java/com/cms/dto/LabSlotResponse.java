package com.cms.dto;

import java.time.Instant;
import java.time.LocalTime;

public record LabSlotResponse(
    Long id,
    String name,
    LocalTime startTime,
    LocalTime endTime,
    Integer slotOrder,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
