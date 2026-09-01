package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClinicalShiftGroupRequest(
    @NotNull(message = "Course offering ID is required")
    Long courseOfferingId,

    Long cohortSectionId,

    @NotBlank(message = "Label is required")
    String label,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Clinical start time is required")
    LocalTime clinicalStartTime
) {}
