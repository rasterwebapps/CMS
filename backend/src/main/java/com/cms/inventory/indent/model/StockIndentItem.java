package com.cms.inventory.indent.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductVariant;
import com.cms.inventory.indent.model.enums.StockIndentItemStatus;

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

/** One product's row on a {@link StockIndent}'s request sheet. */
@Entity
@Table(name = "stock_indent_items")
public class StockIndentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_indent_id", nullable = false)
    private StockIndent stockIndent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Required once {@link #product} has any active variant — see {@code
     *  StockIndentService.resolveVariant}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "requested_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal requestedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockIndentItemStatus status = StockIndentItemStatus.PENDING;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    /**
     * Running total returned back to the issuing location so far (Phase 4's "Internal Return"
     * slice) — only meaningful once {@code status = APPROVED} (issued). Mirrors {@code
     * PurchaseOrderItem.receivedQty}'s "running total on the line itself" shape.
     */
    @Column(name = "returned_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal returnedQty = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StockIndent getStockIndent() { return stockIndent; }
    public void setStockIndent(StockIndent stockIndent) { this.stockIndent = stockIndent; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public BigDecimal getRequestedQty() { return requestedQty; }
    public void setRequestedQty(BigDecimal requestedQty) { this.requestedQty = requestedQty; }

    public StockIndentItemStatus getStatus() { return status; }
    public void setStatus(StockIndentItemStatus status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public BigDecimal getReturnedQty() { return returnedQty; }
    public void setReturnedQty(BigDecimal returnedQty) { this.returnedQty = returnedQty; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
