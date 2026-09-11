package com.cms.inventory.stock.dto;

import java.math.BigDecimal;

public record StockTransferLineResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long variantId,
    String variantCode,
    String variantName,
    String uomCode,
    BigDecimal quantity,
    String notes
) {}
