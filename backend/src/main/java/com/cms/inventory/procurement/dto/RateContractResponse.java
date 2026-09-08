package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RateContractResponse(
    Long id,
    Long supplierId,
    String supplierName,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal contractValueCap,
    String termsText,
    LocalDate renewalReminderDate,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
