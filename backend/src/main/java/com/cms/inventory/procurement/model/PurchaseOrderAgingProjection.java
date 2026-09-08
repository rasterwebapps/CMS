package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;

/**
 * One still-open Purchase Order's total value, as raw input to the PO Aging Report's day-bucket
 * rollup — bucketing itself happens in the service (a plain {@code poDate}-vs-today
 * calculation), this projection just avoids an N+1 query. See {@code
 * PurchaseOrderItemRepository.findOpenOrdersForAging} and the "Purchase Order Aging Report
 * slice" decision-log entry.
 */
public interface PurchaseOrderAgingProjection {
    Long getId();
    String getSupplierName();
    LocalDate getPoDate();
    PurchaseOrderStatus getStatus();
    BigDecimal getTotalValue();
}
