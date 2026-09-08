package com.cms.inventory.procurement.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.procurement.model.enums.PurchaseRequisitionStatus;
import com.cms.inventory.stock.model.InventoryLocation;

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
 * A department/location's request to purchase specific products — the header record; its request
 * sheet is {@link PurchaseRequisitionItem}. Independent of any reorder-level computation (that's a
 * separate, not-yet-built "Wanted List" concept) — this is a human-submitted request that needs
 * per-line sign-off before purchasing can act on it. Mirrors {@code CycleCount}'s shape (manual
 * actor/timestamp fields rather than a generic auditing listener, since this header has several
 * distinct actor/timestamp pairs) more closely than IHMS's flatter, auto-computed
 * {@code StockRequirementItem} — see the "Reference architecture pivot" and "Purchase Requisition
 * slice" decision-log entries.
 */
@Entity
@Table(name = "purchase_requisitions")
public class PurchaseRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRequisitionStatus status = PurchaseRequisitionStatus.DRAFT;

    @Column(name = "requisition_date", nullable = false)
    private LocalDate requisitionDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_by", length = 255)
    private String submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public PurchaseRequisitionStatus getStatus() { return status; }
    public void setStatus(PurchaseRequisitionStatus status) { this.status = status; }

    public LocalDate getRequisitionDate() { return requisitionDate; }
    public void setRequisitionDate(LocalDate requisitionDate) { this.requisitionDate = requisitionDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
