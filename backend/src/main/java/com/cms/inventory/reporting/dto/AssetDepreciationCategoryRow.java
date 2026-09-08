package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;

public record AssetDepreciationCategoryRow(
    Long categoryId,
    String categoryName,
    long assetCount,
    BigDecimal totalPurchaseValue,
    BigDecimal totalAccumulatedDepreciation,
    BigDecimal totalCurrentBookValue
) {}
