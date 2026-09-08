package com.cms.inventory.receiving.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.receiving.model.GoodsReceipt;
import com.cms.inventory.receiving.model.PurchaseOrderCycleTimeProjection;
import com.cms.inventory.receiving.model.enums.GoodsReceiptStatus;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long>, JpaSpecificationExecutor<GoodsReceipt> {

    /** Every fully-received Purchase Order's own {@code poDate} plus the latest confirmation
     *  timestamp among its confirmed Goods Receipts — the PO Cycle-Time Report's raw input. See
     *  {@link PurchaseOrderCycleTimeProjection}. */
    @Query("""
        SELECT po.id AS purchaseOrderId, po.poDate AS poDate, s.id AS supplierId, s.supplierName AS supplierName,
               MAX(gr.confirmedAt) AS lastConfirmedAt
        FROM GoodsReceipt gr
        JOIN gr.purchaseOrder po
        JOIN po.supplier s
        WHERE po.status = :completedStatus AND gr.status = :confirmedStatus
        GROUP BY po.id, po.poDate, s.id, s.supplierName
        """)
    List<PurchaseOrderCycleTimeProjection> findCompletedOrdersForCycleTime(
        @Param("completedStatus") PurchaseOrderStatus completedStatus, @Param("confirmedStatus") GoodsReceiptStatus confirmedStatus);
}
