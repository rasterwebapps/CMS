package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FacultyAvailabilityRequest(

    @NotNull(message = "Faculty is required")
    Long facultyId,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    String reason
) {}
