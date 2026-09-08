package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;

public record StockValuationCategoryRow(
    Long categoryId,
    String categoryName,
    long productCount,
    BigDecimal totalValue
) {}
