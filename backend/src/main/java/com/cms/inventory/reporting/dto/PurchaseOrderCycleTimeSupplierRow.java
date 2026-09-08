package com.cms.inventory.reporting.dto;

public record PurchaseOrderCycleTimeSupplierRow(
    Long supplierId,
    String supplierName,
    long completedOrderCount,
    double averageCycleDays
) {}
