package com.cms.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LabSlotRequest(
    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    @Positive(message = "Slot order must be positive")
    Integer slotOrder,

    Boolean isActive
) {}
