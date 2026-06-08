package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record FeeRefundRequest(
    @NotBlank(message = "Receipt number is required") String receiptNumber,
    @NotBlank(message = "Reason is required") String reason
) {}
