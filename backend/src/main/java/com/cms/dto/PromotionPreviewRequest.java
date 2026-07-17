package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record PromotionPreviewRequest(
    @NotNull(message = "Cohort ID is required") Long cohortId,
    @NotNull(message = "From term instance ID is required") Long fromTermInstanceId,
    @NotNull(message = "To term instance ID is required") Long toTermInstanceId
) {}
