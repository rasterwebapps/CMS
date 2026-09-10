package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TaxSubTypeResponse(
    Long id,
    Long taxRuleId,
    String taxRuleName,
    String jurisdictionMode,
    String componentName,
    BigDecimal splitPercent,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
