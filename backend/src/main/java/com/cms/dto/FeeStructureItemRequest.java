package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

import com.cms.model.enums.FeeType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FeeStructureItemRequest(
    @NotNull(message = "Fee type is required")
    FeeType feeType,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    String description,

    Boolean isMandatory,

    Boolean isActive,

    List<@Valid YearAmountRequest> yearAmounts
) {}
