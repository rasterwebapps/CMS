package com.cms.inventory.approval.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.approval.model.ApprovalWorkflow;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long>, JpaSpecificationExecutor<ApprovalWorkflow> {
}
