package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FeeStructureRequest(
    @NotNull(message = "Program ID is required")
    Long programId,

    @NotNull(message = "Academic Year ID is required")
    Long academicYearId,

    @NotNull(message = "Fee type is required")
    FeeType feeType,

    @PositiveOrZero(message = "Amount must be zero or positive")
    BigDecimal amount,

    String description,

    Boolean isMandatory,

    Boolean isActive,

    Long courseId,

    @NotNull(message = "Quota is required")
    AdmissionQuota quota,

    @NotNull(message = "Fee state ID is required")
    Long feeStateId,

    @NotNull(message = "Gender is required")
    Gender gender,

    List<@Valid YearAmountRequest> yearAmounts
) {}
