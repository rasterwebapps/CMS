package com.cms.inventory.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.PurchaseRequisition;

@Repository
public interface PurchaseRequisitionRepository
        extends JpaRepository<PurchaseRequisition, Long>, JpaSpecificationExecutor<PurchaseRequisition> {
}
