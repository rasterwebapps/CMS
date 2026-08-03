package com.cms.dto;

import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.HolidayCategory;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HolidayTemplateRequest(
    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Recurrence type is required")
    HolidayRecurrenceType recurrenceType,

    HolidayCategory holidayCategory,

    String description,

    /** Defaults to 1 (single-day) if null. */
    Integer durationDays,

    /** YEARLY only: 1-12. */
    Integer month,

    /** YEARLY only: 1-31. */
    Integer dayOfMonth,

    /** MONTHLY only. */
    WeekOfMonth weekOfMonth,

    /** MONTHLY only. */
    DayOfWeek dayOfWeek,

    Boolean isActive
) {}
