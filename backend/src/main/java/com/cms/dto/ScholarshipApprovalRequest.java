package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.DisbursementFrequency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ScholarshipApprovalRequest(
    @NotNull(message = "Approved amount is required")
    @PositiveOrZero(message = "Approved amount must be zero or positive")
    BigDecimal approvedAmount,

    DisbursementFrequency disbursementFrequency,
    LocalDate validFrom,
    LocalDate validTill,
    String remarks
) {}

