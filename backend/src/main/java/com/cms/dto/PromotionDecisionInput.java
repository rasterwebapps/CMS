package com.cms.dto;

import com.cms.model.enums.PromotionOutcome;

import jakarta.validation.constraints.NotNull;

public record PromotionDecisionInput(
    @NotNull(message = "Student ID is required") Long studentId,
    @NotNull(message = "Outcome is required") PromotionOutcome outcome,
    String remarks
) {}
