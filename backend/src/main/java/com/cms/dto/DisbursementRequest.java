package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.DisbursementMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DisbursementRequest(
    Long academicYearId,
    Integer semesterNumber,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Disbursement date is required")
    LocalDate disbursementDate,

    @NotNull(message = "Disbursement mode is required")
    DisbursementMode disbursementMode,

    String transactionReference,
    String chequeNumber,
    String bankName,
    String remarks
) {}

