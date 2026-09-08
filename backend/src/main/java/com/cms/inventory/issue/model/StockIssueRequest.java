package com.cms.inventory.issue.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.issue.model.enums.StockIssueRequestStatus;
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
 * Phase 4's ("Requests, Issues & Returns") first slice — a requesting {@link InventoryLocation}
 * (typically a department/ward with {@code locationRole = REQUESTING_POINT}) asks an issuing
 * {@link InventoryLocation} (typically a {@code STORE}) for on-hand stock. <strong>Naming
 * caution:</strong> distinct from Phase 2's {@code PurchaseRequisition} (a request to
 * <em>buy</em> from a supplier) — this requests already-on-hand stock from within the
 * organization; never conflate the two in code, nav, or permission names (see the
 * `AUTONOMOUS_OVERNIGHT_PLAN.md` note this slice was built from). Header/line shape (DRAFT ->
 * SUBMITTED -> COMPLETED/CANCELLED, per-line PENDING -> APPROVED/REJECTED) mirrors {@code
 * PurchaseRequisition}/{@code PurchaseRequisitionItem} almost exactly — the closest in-repo
 * precedent — except approving a line here also posts a real {@code ISSUE} stock movement
 * (decreasing the issuing location's balance) rather than just reaching a terminal sign-off
 * state with nothing yet to pick up downstream. See the "Stock Issue Request slice" decision-log
 * entry.
 */
@Entity
@Table(name = "stock_issue_requests")
public class StockIssueRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_location_id", nullable = false)
    private InventoryLocation requestingLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuing_location_id", nullable = false)
    private InventoryLocation issuingLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockIssueRequestStatus status = StockIssueRequestStatus.DRAFT;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

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

    public InventoryLocation getRequestingLocation() { return requestingLocation; }
    public void setRequestingLocation(InventoryLocation requestingLocation) { this.requestingLocation = requestingLocation; }

    public InventoryLocation getIssuingLocation() { return issuingLocation; }
    public void setIssuingLocation(InventoryLocation issuingLocation) { this.issuingLocation = issuingLocation; }

    public StockIssueRequestStatus getStatus() { return status; }
    public void setStatus(StockIssueRequestStatus status) { this.status = status; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

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
