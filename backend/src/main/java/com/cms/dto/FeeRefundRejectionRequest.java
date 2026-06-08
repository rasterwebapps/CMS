package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record FeeRefundRejectionRequest(
    @NotBlank(message = "Rejection reason is required") String rejectionReason
) {}
