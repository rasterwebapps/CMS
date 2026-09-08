package com.cms.inventory.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single at-a-glance overview of the whole Inventory Management module — every field is
 * computed live from existing entities, nothing is stored. See the "Inventory Dashboard slice"
 * decision-log entry for what each metric means and why it was chosen.
 */
public record InventoryDashboardResponse(
    long openPurchaseOrders,
    long pendingPurchaseRequisitions,
    long pendingWantedListItems,
    long activeApprovalInstances,
    long overdueGatePasses,
    long overdueLoanableItems,
    long openServiceTickets,
    long urgentOpenServiceTickets,
    long overAllocatedBudgets,
    BigDecimal consignmentOutstandingLiability,
    long assetsUnderMaintenance,
    Instant generatedAt
) {}
