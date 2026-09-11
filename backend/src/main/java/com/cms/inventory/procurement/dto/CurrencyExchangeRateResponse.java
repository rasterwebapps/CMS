package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CurrencyExchangeRateResponse(
    Long id,
    String currencyCode,
    BigDecimal rateToBase,
    LocalDate effectiveDate,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
