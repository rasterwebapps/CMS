package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
    Long id,
    String productCode,
    String productName,
    Long categoryId,
    String categoryName,
    Long baseUomId,
    String baseUomCode,
    String baseUomName,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    Boolean isAsset,
    Boolean isConsumable,
    Boolean isService,
    Boolean isLoanable,
    BigDecimal depreciationRate,
    Integer warrantyPeriodMonths,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    List<String> aliases,
    List<ProductAttributeValueResponse> attributeValues
) {}
