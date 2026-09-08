package com.cms.inventory.gatepass.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.gatepass.model.enums.GatePassDirection;
import com.cms.inventory.gatepass.model.enums.GatePassStatus;
import com.cms.inventory.procurement.model.PurchaseOrder;
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
 * Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") first slice — tracks a
 * {@link Product} or {@link Asset} physically leaving or entering the premises outside a normal
 * stock movement (repair send-outs, event/sports equipment loan-out, a vendor's own tool brought
 * in for an on-site job), per the reference architecture ({@code
 * ER_DIAGRAM_AND_MODULE_BOUNDARIES.md} §6). Exactly one of {@code product}/{@code asset} is set
 * (validated in the service, matching the ER doc's own "ProductId or AssetId" shape). Approval
 * and gate (security) verification are always two distinct actions/actors even when the same
 * person holds both permissions — see {@link GatePassStatus}. "Overdue" is derived at read time
 * from {@code expectedReturnDate}, exactly like {@code LoanableItemIssue}, never stored. See the
 * "Gate Pass slice" decision-log entry.
 */
@Entity
@Table(name = "gate_passes")
public class GatePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GatePassDirection direction;

    @Column(nullable = false)
    private boolean returnable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "party_name", nullable = false, length = 200)
    private String partyName;

    @Column(name = "party_contact", length = 100)
    private String partyContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_purchase_order_id")
    private PurchaseOrder linkedPurchaseOrder;

    @Column(name = "pass_date", nullable = false)
    private LocalDate passDate;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GatePassStatus status = GatePassStatus.PENDING_APPROVAL;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by", length = 255)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "gate_verified_by", length = 255)
    private String gateVerifiedBy;

    @Column(name = "gate_verified_at")
    private Instant gateVerifiedAt;

    @Column(name = "returned_by", length = 255)
    private String returnedBy;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GatePassDirection getDirection() { return direction; }
    public void setDirection(GatePassDirection direction) { this.direction = direction; }

    public boolean isReturnable() { return returnable; }
    public void setReturnable(boolean returnable) { this.returnable = returnable; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public String getPartyContact() { return partyContact; }
    public void setPartyContact(String partyContact) { this.partyContact = partyContact; }

    public PurchaseOrder getLinkedPurchaseOrder() { return linkedPurchaseOrder; }
    public void setLinkedPurchaseOrder(PurchaseOrder linkedPurchaseOrder) { this.linkedPurchaseOrder = linkedPurchaseOrder; }

    public LocalDate getPassDate() { return passDate; }
    public void setPassDate(LocalDate passDate) { this.passDate = passDate; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDate getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }

    public GatePassStatus getStatus() { return status; }
    public void setStatus(GatePassStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }

    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getGateVerifiedBy() { return gateVerifiedBy; }
    public void setGateVerifiedBy(String gateVerifiedBy) { this.gateVerifiedBy = gateVerifiedBy; }

    public Instant getGateVerifiedAt() { return gateVerifiedAt; }
    public void setGateVerifiedAt(Instant gateVerifiedAt) { this.gateVerifiedAt = gateVerifiedAt; }

    public String getReturnedBy() { return returnedBy; }
    public void setReturnedBy(String returnedBy) { this.returnedBy = returnedBy; }

    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
