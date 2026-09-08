package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StockValuationReportResponse(
    List<StockValuationCategoryRow> categories,
    long grandTotalProductCount,
    BigDecimal grandTotalValue,
    Instant generatedAt
) {}
