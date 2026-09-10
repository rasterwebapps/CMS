package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.PurchaseOrderItemTaxComponent;

@Repository
public interface PurchaseOrderItemTaxComponentRepository extends JpaRepository<PurchaseOrderItemTaxComponent, Long> {

    List<PurchaseOrderItemTaxComponent> findByPurchaseOrderItem_IdOrderByIdAsc(Long purchaseOrderItemId);
}
