package com.cms.inventory.procurement.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);

    /**
     * Committed spend for a location within a date range — every line on a PO that has actually
     * been sent (status not {@code PENDING}; {@code FORCE_CLOSED} still counts, it represents
     * real spend up to that point). Used by {@code BudgetService} to compute "consumed" against
     * an allocation — see the "Budget allocation slice" decision-log entry.
     */
    @Query("SELECT COALESCE(SUM(i.lineTotal), 0) FROM PurchaseOrderItem i "
        + "WHERE i.purchaseOrder.location.id = :locationId "
        + "AND i.purchaseOrder.poDate BETWEEN :startDate AND :endDate "
        + "AND i.purchaseOrder.status <> :excludedStatus")
    BigDecimal sumCommittedSpendForLocationAndDateRange(
        Long locationId, LocalDate startDate, LocalDate endDate, PurchaseOrderStatus excludedStatus);
}
