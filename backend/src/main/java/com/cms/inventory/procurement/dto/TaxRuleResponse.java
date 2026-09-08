package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TaxRuleResponse(
    Long id,
    String name,
    BigDecimal ratePercent,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
