package com.cms.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PeriodRequest(

    @NotBlank(message = "Period name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be a positive number of minutes")
    Integer durationMinutes,

    Integer periodOrder,

    Boolean isActive
) {}
