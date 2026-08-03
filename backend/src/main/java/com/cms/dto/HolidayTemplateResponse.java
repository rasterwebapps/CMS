package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.HolidayCategory;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;

public record HolidayTemplateResponse(
    Long id,
    String name,
    HolidayRecurrenceType recurrenceType,
    HolidayCategory holidayCategory,
    String description,
    Integer durationDays,
    Integer month,
    Integer dayOfMonth,
    WeekOfMonth weekOfMonth,
    DayOfWeek dayOfWeek,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
