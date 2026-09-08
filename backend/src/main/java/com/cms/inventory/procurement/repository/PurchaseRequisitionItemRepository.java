package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.ProductLocationQtyProjection;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;

@Repository
public interface PurchaseRequisitionItemRepository extends JpaRepository<PurchaseRequisitionItem, Long> {

    List<PurchaseRequisitionItem> findByPurchaseRequisitionIdOrderByIdAsc(Long purchaseRequisitionId);

    boolean existsByPurchaseRequisitionIdAndProductId(Long purchaseRequisitionId, Long productId);

    boolean existsByPurchaseRequisitionIdAndStatus(Long purchaseRequisitionId, PurchaseRequisitionItemStatus status);

    /**
     * Quantity already "in the pipeline" per (product, location) — an MRP-standard netting term,
     * adapted here since Purchase Order doesn't exist yet: an item counts as open once its
     * requisition has been submitted (APPROVED lines always sit under a SUBMITTED/COMPLETED
     * header; PENDING lines only count once the header itself is SUBMITTED — a still-DRAFT
     * header's lines aren't a firm commitment yet and are excluded). Used by the Wanted List
     * shortage job so it never re-flags a shortfall that's already been requested.
     */
    @Query(value = """
        SELECT i.product_id AS productId, r.location_id AS locationId, SUM(i.requested_qty) AS qty
        FROM purchase_requisition_items i
        JOIN purchase_requisitions r ON r.id = i.purchase_requisition_id
        WHERE i.status = 'APPROVED' OR (i.status = 'PENDING' AND r.status = 'SUBMITTED')
        GROUP BY i.product_id, r.location_id
        """, nativeQuery = true)
    List<ProductLocationQtyProjection> findOpenQtyByProductAndLocation();
}
