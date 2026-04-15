package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExperimentRequest(
    @NotNull(message = "Subject ID is required")
    Long subjectId,

    @NotNull(message = "Experiment number is required")
    @Positive(message = "Experiment number must be positive")
    Integer experimentNumber,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @Size(max = 1000, message = "Aim must not exceed 1000 characters")
    String aim,

    @Size(max = 2000, message = "Apparatus must not exceed 2000 characters")
    String apparatus,

    @Size(max = 4000, message = "Procedure must not exceed 4000 characters")
    String procedure,

    @Size(max = 1000, message = "Expected outcome must not exceed 1000 characters")
    String expectedOutcome,

    @Size(max = 2000, message = "Learning outcomes must not exceed 2000 characters")
    String learningOutcomes,

    @Positive(message = "Estimated duration must be positive")
    Integer estimatedDurationMinutes,

    Boolean isActive
) {}
