package com.cms.inventory.issue.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockIssueRequestItemResponse(
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
