package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.BlockType;
import com.cms.model.enums.DayOfWeek;

public record BlockedPeriodResponse(
    Long id,
    Long periodId,
    String periodName,
    BlockType blockType,
    LocalDate specificDate,
    DayOfWeek dayOfWeek,
    LocalDate rangeStartDate,
    LocalDate rangeEndDate,
    String reason,
    Instant createdAt,
    Instant updatedAt
) {}