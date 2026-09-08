package com.cms.inventory.reporting.dto;

import java.time.Instant;
import java.util.List;

public record PurchaseOrderCycleTimeReportResponse(
    List<PurchaseOrderCycleTimeSupplierRow> suppliers,
    long grandTotalCompletedOrderCount,
    double overallAverageCycleDays,
    Instant generatedAt
) {}
