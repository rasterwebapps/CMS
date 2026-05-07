package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record ScholarshipRejectionRequest(
    @NotBlank(message = "Rejection reason is required")
    String reason
) {}

