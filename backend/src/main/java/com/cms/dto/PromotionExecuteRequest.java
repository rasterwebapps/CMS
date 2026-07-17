package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PromotionExecuteRequest(
    @NotNull(message = "Cohort ID is required") Long cohortId,
    @NotNull(message = "From term instance ID is required") Long fromTermInstanceId,
    @NotNull(message = "To term instance ID is required") Long toTermInstanceId,
    @NotEmpty(message = "At least one decision is required") List<PromotionDecisionInput> decisions,
    boolean generateCourseRegistrations,
    boolean generateFeeDemands
) {}
