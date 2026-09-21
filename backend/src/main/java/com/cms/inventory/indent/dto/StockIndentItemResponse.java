package com.cms.inventory.indent.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockIndentItemResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long variantId,
    String variantCode,
    String variantName,
    String uomCode,
    BigDecimal requestedQty,
    String status,
    String resolvedBy,
    Instant resolvedAt,
    String resolutionNotes,
    BigDecimal returnedQty,
    String notes
) {}
