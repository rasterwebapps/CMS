package com.cms.inventory.receiving.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One fully-received (COMPLETED) Purchase Order's raw cycle-time inputs — its own {@code
 * poDate} plus the latest confirmation timestamp among its {@code CONFIRMED} Goods Receipts
 * (the moment its last line was fully received, i.e. the moment it became COMPLETED). Days
 * elapsed between the two is computed in the service, one row averaged per supplier. See {@code
 * GoodsReceiptRepository.findCompletedOrdersForCycleTime} and the "Purchase Order Cycle-Time
 * Report slice" decision-log entry.
 */
public interface PurchaseOrderCycleTimeProjection {
    Long getPurchaseOrderId();
    LocalDate getPoDate();
    Long getSupplierId();
    String getSupplierName();
    Instant getLastConfirmedAt();
}
