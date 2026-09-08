package com.cms.inventory.approval.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.cms.inventory.approval.model.enums.ApprovalInstanceStatus;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseRequisition;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * One document actually routed through an {@link ApprovalWorkflow}. Exactly one of {@code
 * purchaseRequisition}/{@code purchaseOrder} is populated, matching the workflow's own {@code
 * documentType} — a real FK per possible target (only two exist) rather than a polymorphic
 * {@code entityType}/{@code entityId} soft reference, per this module's own "a real FK is
 * strictly better than a soft reference when the target set is small and known" precedent (the
 * Stock Tracking slice's `InventoryLocation` decision). Starting an instance is a deliberate,
 * explicit action a user takes on a document already sitting in its own normal state machine —
 * this instance's progress does not itself change that document's status; see the class docs on
 * {@link ApprovalWorkflow} for the full scope boundary. See the "Multi-level approval routing
 * slice" decision-log entry.
 */
@Entity
@Table(name = "approval_instances")
public class ApprovalInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_id")
    private PurchaseRequisition purchaseRequisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalInstanceStatus status = ApprovalInstanceStatus.IN_PROGRESS;

    @Column(name = "current_step_order", nullable = false)
    private Integer currentStepOrder;

    @Column(name = "initiated_by", length = 255)
    private String initiatedBy;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ApprovalAction> actions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApprovalWorkflow getWorkflow() { return workflow; }
    public void setWorkflow(ApprovalWorkflow workflow) { this.workflow = workflow; }

    public PurchaseRequisition getPurchaseRequisition() { return purchaseRequisition; }
    public void setPurchaseRequisition(PurchaseRequisition purchaseRequisition) { this.purchaseRequisition = purchaseRequisition; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public ApprovalInstanceStatus getStatus() { return status; }
    public void setStatus(ApprovalInstanceStatus status) { this.status = status; }

    public Integer getCurrentStepOrder() { return currentStepOrder; }
    public void setCurrentStepOrder(Integer currentStepOrder) { this.currentStepOrder = currentStepOrder; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ApprovalAction> getActions() { return actions; }
    public void setActions(List<ApprovalAction> actions) { this.actions = actions; }
}
