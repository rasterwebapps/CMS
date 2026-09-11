package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductVariantResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String variantCode,
    String variantName,
    String barcode,
    String trackingMode,
    BigDecimal standardCost,
    BigDecimal listPrice,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    List<ProductAttributeValueResponse> attributeValues
) {}
