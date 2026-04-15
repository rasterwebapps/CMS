package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReferralTypeResponse(
    Long id,
    String name,
    String code,
    BigDecimal guidelineValue,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
