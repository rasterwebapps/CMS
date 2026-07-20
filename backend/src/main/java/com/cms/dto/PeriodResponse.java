package com.cms.dto;

import java.time.Instant;
import java.time.LocalTime;

public record PeriodResponse(
    Long id,
    String name,
    LocalTime startTime,
    LocalTime endTime,
    Integer periodOrder,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
