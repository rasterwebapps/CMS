package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.DayOfWeek;

public record DayMappingOverrideResponse(
    Long id,
    Long termInstanceId,
    LocalDate mappedDate,
    DayOfWeek borrowedDayOfWeek,
    String reason,
    Instant createdAt,
    Instant updatedAt
) {}
