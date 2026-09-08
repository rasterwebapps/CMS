package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;

public record PurchaseOrderAgingBucketRow(
    String bucketLabel,
    long orderCount,
    BigDecimal totalValue
) {}
