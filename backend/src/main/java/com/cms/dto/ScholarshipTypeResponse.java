package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.DiscountType;
import com.cms.model.enums.ScholarshipApplicationMode;

public record ScholarshipTypeResponse(
    Long id,
    String code,
    String name,
    String description,
    Boolean govtScheme,
    String schemeCode,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal maxAmountPerYear,
    Boolean renewalRequired,
    Boolean active,
    ScholarshipApplicationMode applicationMode,
    String portalName,
    String portalUrl,
    Integer eligibleFromYear,
    Integer eligibleToYear,
    Instant createdAt,
    Instant updatedAt
) {}

