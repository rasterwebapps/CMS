package com.cms.inventory.approval.model;

import java.time.Instant;

import com.cms.inventory.approval.model.enums.ApprovalActionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One {@link ApprovalWorkflowStep}'s sign-off within one {@link ApprovalInstance}. A row is
 * created for every step of the workflow up front, at instance-creation time (not lazily as the
 * chain progresses) — all {@code PENDING}, but only actionable once the instance's own {@code
 * currentStepOrder} reaches this action's step's {@code stepOrder} (enforced in {@code
 * ApprovalInstanceService}, not by hiding rows, so the whole chain's shape is visible up front).
 * See the "Multi-level approval routing slice" decision-log entry.
 */
@Entity
@Table(name = "approval_actions")
public class ApprovalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private ApprovalInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private ApprovalWorkflowStep workflowStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalActionStatus status = ApprovalActionStatus.PENDING;

    @Column(name = "acted_by", length = 255)
    private String actedBy;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApprovalInstance getInstance() { return instance; }
    public void setInstance(ApprovalInstance instance) { this.instance = instance; }

    public ApprovalWorkflowStep getWorkflowStep() { return workflowStep; }
    public void setWorkflowStep(ApprovalWorkflowStep workflowStep) { this.workflowStep = workflowStep; }

    public ApprovalActionStatus getStatus() { return status; }
    public void setStatus(ApprovalActionStatus status) { this.status = status; }

    public String getActedBy() { return actedBy; }
    public void setActedBy(String actedBy) { this.actedBy = actedBy; }

    public Instant getActedAt() { return actedAt; }
    public void setActedAt(Instant actedAt) { this.actedAt = actedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
