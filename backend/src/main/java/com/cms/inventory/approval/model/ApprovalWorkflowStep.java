package com.cms.inventory.approval.model;

import com.cms.model.Permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One stage of an {@link ApprovalWorkflow}. Sequential vs. parallel routing is represented with
 * a single flat {@code stepOrder} column, no separate "mode" flag: steps sharing the same {@code
 * stepOrder} within one workflow run in parallel (every one of them must approve before that
 * stage completes); distinct {@code stepOrder} values run sequentially, in ascending order. Who
 * may act on a step is {@code permission} — a real FK into the existing, DB-driven {@code
 * permissions} table (the same primitive every {@code @PreAuthorize} check in this app already
 * uses), not a hardcoded role or a free-text name — reusing the platform's existing permission
 * system rather than inventing a parallel "approver" concept. See the "Multi-level approval
 * routing slice" decision-log entry.
 */
@Entity
@Table(name = "approval_workflow_steps")
public class ApprovalWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", nullable = false, length = 150)
    private String stepName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApprovalWorkflow getWorkflow() { return workflow; }
    public void setWorkflow(ApprovalWorkflow workflow) { this.workflow = workflow; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public Permission getPermission() { return permission; }
    public void setPermission(Permission permission) { this.permission = permission; }
}
