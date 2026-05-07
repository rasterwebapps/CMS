package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.DiscountType;
import com.cms.model.enums.ScholarshipApplicationMode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ScholarshipTypeRequest(
    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    String code,

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    String description,
    Boolean govtScheme,
    String schemeCode,

    @NotNull(message = "Discount type is required")
    DiscountType discountType,

    @PositiveOrZero(message = "Discount value must be zero or positive")
    BigDecimal discountValue,

    @PositiveOrZero(message = "Maximum amount must be zero or positive")
    BigDecimal maxAmountPerYear,

    Boolean renewalRequired,
    Boolean active,

    /** Defaults to INSTITUTION if not supplied. */
    ScholarshipApplicationMode applicationMode,

    @Size(max = 50, message = "Portal name must not exceed 50 characters")
    String portalName,

    @Size(max = 255, message = "Portal URL must not exceed 255 characters")
    String portalUrl,

    @Min(value = 1, message = "Eligible from year must be at least 1")
    Integer eligibleFromYear,

    @Min(value = 1, message = "Eligible to year must be at least 1")
    Integer eligibleToYear
) {}

