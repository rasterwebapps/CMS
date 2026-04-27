package com.cms.dto;

import com.cms.model.enums.ProgramStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProgramRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @NotNull(message = "Duration years is required")
    @Min(value = 1, message = "Duration must be at least 1 year")
    @Max(value = 10, message = "Duration must not exceed 10 years")
    Integer durationYears,

    ProgramStatus status
) {}
