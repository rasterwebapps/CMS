package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.CalendarEventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalendarEventRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @NotNull(message = "Event type is required")
    CalendarEventType eventType,

    @NotNull(message = "Academic year ID is required")
    Long academicYearId,

    Long semesterId
) {}
