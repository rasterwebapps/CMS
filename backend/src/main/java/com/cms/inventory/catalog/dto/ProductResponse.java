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
    Long brandId,
    String brandName,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    Boolean isAsset,
    Boolean isConsumable,
    Boolean isService,
    Boolean isLoanable,
    String trackingMode,
    BigDecimal depreciationRate,
    Integer warrantyPeriodMonths,
    BigDecimal lengthCm,
    BigDecimal widthCm,
    BigDecimal heightCm,
    BigDecimal weightKg,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    List<String> aliases,
    List<ProductAttributeValueResponse> attributeValues
) {}
