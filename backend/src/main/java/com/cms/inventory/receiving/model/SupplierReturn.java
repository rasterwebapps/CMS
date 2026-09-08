package com.cms.inventory.receiving.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.receiving.model.enums.SupplierReturnReason;
import com.cms.inventory.receiving.model.enums.SupplierReturnStatus;

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
 * Returns previously-received stock to the supplier it came from — Phase 3's ("Receiving & Stock
 * Movement") third and final slice, closing the phase. Raised against one {@link GoodsReceipt}
 * that has been {@code CONFIRMED} (only a confirmed receipt has actually posted stock that can be
 * returned). DRAFT (build lines) -> COMPLETED posts a decreasing {@code RETURN} stock movement per
 * line through the existing {@code StockMovementService}, and nets each line's quantity back out
 * of its source {@code PurchaseOrderItem.receivedQty} so the parent order's own status (via {@code
 * PurchaseOrderService.recalculateReceiptProgress}) reflects the true accepted quantity — a
 * previously {@code COMPLETED} order can correctly revert to {@code PARTIALLY_COMPLETED}. See the
 * "Return to Supplier slice" decision-log entry.
 */
@Entity
@Table(name = "supplier_returns")
public class SupplierReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierReturnStatus status = SupplierReturnStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SupplierReturnReason reason;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GoodsReceipt getGoodsReceipt() { return goodsReceipt; }
    public void setGoodsReceipt(GoodsReceipt goodsReceipt) { this.goodsReceipt = goodsReceipt; }

    public SupplierReturnStatus getStatus() { return status; }
    public void setStatus(SupplierReturnStatus status) { this.status = status; }

    public SupplierReturnReason getReason() { return reason; }
    public void setReason(SupplierReturnReason reason) { this.reason = reason; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
