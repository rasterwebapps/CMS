package com.cms.inventory.procurement.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.PurchaseOrderAgingProjection;
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

    /**
     * Every still-open (not {@code COMPLETED}/{@code FORCE_CLOSED}) Purchase Order's total
     * value, one row per order — the PO Aging Report's raw input, bucketed by {@code poDate}
     * age in the service. See {@link PurchaseOrderAgingProjection}.
     */
    @Query("""
        SELECT po.id AS id, s.supplierName AS supplierName, po.poDate AS poDate, po.status AS status,
               COALESCE(SUM(i.lineTotal), 0) AS totalValue
        FROM PurchaseOrderItem i
        JOIN i.purchaseOrder po
        JOIN po.supplier s
        WHERE po.status IN :openStatuses
        GROUP BY po.id, s.supplierName, po.poDate, po.status
        """)
    List<PurchaseOrderAgingProjection> findOpenOrdersForAging(@Param("openStatuses") List<PurchaseOrderStatus> openStatuses);
}
