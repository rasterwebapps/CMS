package com.cms.inventory.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.approval.model.ApprovalWorkflowStep;

@Repository
public interface ApprovalWorkflowStepRepository extends JpaRepository<ApprovalWorkflowStep, Long> {

    List<ApprovalWorkflowStep> findByWorkflowIdOrderByStepOrderAscIdAsc(Long workflowId);
}
