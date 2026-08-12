package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record SpecialClassRejectionRequest(
    @NotBlank(message = "Rejection reason is required") String rejectionReason
) {}
