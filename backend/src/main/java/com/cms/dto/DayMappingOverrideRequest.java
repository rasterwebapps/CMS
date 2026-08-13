package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DayMappingOverrideRequest(
    @NotNull(message = "Term instance ID is required")
    Long termInstanceId,

    @NotNull(message = "Mapped date is required")
    LocalDate mappedDate,

    @NotNull(message = "Borrowed day of week is required")
    DayOfWeek borrowedDayOfWeek,

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    String reason
) {}
