package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseRequisitionItemResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    BigDecimal requestedQty,
    String status,
    String resolvedBy,
    Instant resolvedAt,
    String resolutionNotes,
    String notes
) {}
