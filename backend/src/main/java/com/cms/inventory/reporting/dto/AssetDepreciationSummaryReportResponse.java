package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AssetDepreciationSummaryReportResponse(
    List<AssetDepreciationCategoryRow> categories,
    long grandTotalAssetCount,
    BigDecimal grandTotalPurchaseValue,
    BigDecimal grandTotalAccumulatedDepreciation,
    BigDecimal grandTotalCurrentBookValue,
    Instant generatedAt
) {}
