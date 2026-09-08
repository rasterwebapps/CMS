package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;

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

/** One product's row on a {@link PurchaseRequisition}'s request sheet. */
@Entity
@Table(name = "purchase_requisition_items")
public class PurchaseRequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_id", nullable = false)
    private PurchaseRequisition purchaseRequisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "requested_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal requestedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRequisitionItemStatus status = PurchaseRequisitionItemStatus.PENDING;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseRequisition getPurchaseRequisition() { return purchaseRequisition; }
    public void setPurchaseRequisition(PurchaseRequisition purchaseRequisition) { this.purchaseRequisition = purchaseRequisition; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getRequestedQty() { return requestedQty; }
    public void setRequestedQty(BigDecimal requestedQty) { this.requestedQty = requestedQty; }

    public PurchaseRequisitionItemStatus getStatus() { return status; }
    public void setStatus(PurchaseRequisitionItemStatus status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
