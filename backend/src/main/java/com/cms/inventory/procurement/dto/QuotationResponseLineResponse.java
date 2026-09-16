package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record QuotationResponseLineResponse(
    Long id,
    Long supplierId,
    String supplierName,
    BigDecimal quotedUnitPrice,
    Integer quotedLeadTimeDays,
    String notes,
    String recordedBy,
    Instant recordedAt
) {}
