package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WantedListItemResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    Long locationId,
    String locationVirtualName,
    String status,
    BigDecimal qtyOnHandSnapshot,
    BigDecimal qtyOnOrderSnapshot,
    BigDecimal reorderLevelSnapshot,
    BigDecimal suggestedQty,
    Instant generatedAt,
    String resolvedBy,
    Instant resolvedAt,
    String resolutionNotes,
    String rejectionReason,
    Long convertedPurchaseRequisitionId,
    Long convertedPurchaseRequisitionItemId
) {}
