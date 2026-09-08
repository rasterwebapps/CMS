package com.cms.inventory.approval.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.approval.model.ApprovalInstance;
import com.cms.inventory.approval.model.enums.ApprovalInstanceStatus;

@Repository
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long>, JpaSpecificationExecutor<ApprovalInstance> {

    Optional<ApprovalInstance> findFirstByPurchaseRequisitionIdAndStatus(Long purchaseRequisitionId, ApprovalInstanceStatus status);

    Optional<ApprovalInstance> findFirstByPurchaseOrderIdAndStatus(Long purchaseOrderId, ApprovalInstanceStatus status);
}
