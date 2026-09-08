package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;

@Repository
public interface PurchaseRequisitionItemRepository extends JpaRepository<PurchaseRequisitionItem, Long> {

    List<PurchaseRequisitionItem> findByPurchaseRequisitionIdOrderByIdAsc(Long purchaseRequisitionId);

    boolean existsByPurchaseRequisitionIdAndProductId(Long purchaseRequisitionId, Long productId);

    boolean existsByPurchaseRequisitionIdAndStatus(Long purchaseRequisitionId, PurchaseRequisitionItemStatus status);
}
