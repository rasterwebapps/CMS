package com.cms.inventory.approval.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.approval.model.enums.ApprovalDocumentType;
import com.cms.inventory.stock.model.InventoryLocation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * Phase 6's ("Budgets & Approvals") second and largest slice — a reusable, named approval-chain
 * definition: which document type it applies to, an optional {@code location} scope (null =
 * every location) and, for {@code PURCHASE_ORDER} only, an optional {@code minAmount} threshold
 * (null = applies regardless of amount; {@code PURCHASE_REQUISITION} carries no monetary value,
 * so a threshold makes no sense there and stays unused for that type). {@code steps} defines the
 * actual chain — see {@link ApprovalWorkflowStep}'s docs for how sequential vs. parallel routing
 * is represented. Deliberately an <em>optional, standalone</em> capability, not a rewrite of
 * {@code PurchaseRequisitionService}/{@code PurchaseOrderService}'s own existing single-permission
 * approve/reject — see the "Multi-level approval routing slice" decision-log entry for the exact
 * scope boundary (what this slice does and does not touch).
 */
@Entity
@Table(name = "approval_workflows")
@EntityListeners(AuditingEntityListener.class)
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private ApprovalDocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Column(name = "min_amount", precision = 14, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC, id ASC")
    private List<ApprovalWorkflowStep> steps = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ApprovalDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(ApprovalDocumentType documentType) { this.documentType = documentType; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<ApprovalWorkflowStep> getSteps() { return steps; }
    public void setSteps(List<ApprovalWorkflowStep> steps) { this.steps = steps; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
