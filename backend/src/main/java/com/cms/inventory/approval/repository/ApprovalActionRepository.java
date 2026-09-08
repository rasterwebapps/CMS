package com.cms.inventory.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.approval.model.ApprovalAction;
import com.cms.inventory.approval.model.enums.ApprovalActionStatus;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {

    List<ApprovalAction> findByInstanceIdOrderByIdAsc(Long instanceId);

    List<ApprovalAction> findByInstanceIdAndWorkflowStep_StepOrder(Long instanceId, Integer stepOrder);

    boolean existsByInstanceIdAndWorkflowStep_StepOrderAndStatus(Long instanceId, Integer stepOrder, ApprovalActionStatus status);
}
