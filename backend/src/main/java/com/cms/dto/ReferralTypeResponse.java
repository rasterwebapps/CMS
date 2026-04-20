package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReferralTypeResponse(
    Long id,
    String name,
    String code,
    BigDecimal commissionAmount,
    Boolean hasCommission,
    String description,
    Boolean isActive,
    Boolean isSystemDefined,
    Instant createdAt,
    Instant updatedAt
) {}
