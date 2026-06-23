package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record CommissionRejectionRequest(
    @NotBlank(message = "A rejection reason is required")
    String reason
) {}
