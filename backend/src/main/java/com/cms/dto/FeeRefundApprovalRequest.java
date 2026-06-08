package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeeRefundApprovalRequest(
    @NotBlank(message = "Payment mode is required") String paymentMode,
    @NotNull(message = "Payment date is required") LocalDate paymentDate,
    String transactionReference
) {}
