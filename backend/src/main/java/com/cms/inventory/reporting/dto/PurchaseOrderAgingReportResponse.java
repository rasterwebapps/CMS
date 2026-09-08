package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderAgingReportResponse(
    List<PurchaseOrderAgingBucketRow> buckets,
    long grandTotalOrderCount,
    BigDecimal grandTotalValue,
    Instant generatedAt
) {}
