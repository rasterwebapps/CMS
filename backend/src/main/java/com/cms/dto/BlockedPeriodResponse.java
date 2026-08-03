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
    Instant updatedAt,
    /** Non-null only when this block was auto-generated from a HOLIDAY CalendarEvent -- the
     *  frontend uses this alone (no separate boolean) to render an "Auto · Holiday" tag. */
    Long sourceCalendarEventId
) {}