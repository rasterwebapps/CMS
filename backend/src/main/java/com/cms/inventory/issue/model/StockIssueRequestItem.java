package com.cms.inventory.issue.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.issue.model.enums.StockIssueRequestItemStatus;

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

/** One product's row on a {@link StockIssueRequest}'s request sheet. */
@Entity
@Table(name = "stock_issue_request_items")
public class StockIssueRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_issue_request_id", nullable = false)
    private StockIssueRequest stockIssueRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "requested_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal requestedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockIssueRequestItemStatus status = StockIssueRequestItemStatus.PENDING;

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

    public StockIssueRequest getStockIssueRequest() { return stockIssueRequest; }
    public void setStockIssueRequest(StockIssueRequest stockIssueRequest) { this.stockIssueRequest = stockIssueRequest; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getRequestedQty() { return requestedQty; }
    public void setRequestedQty(BigDecimal requestedQty) { this.requestedQty = requestedQty; }

    public StockIssueRequestItemStatus getStatus() { return status; }
    public void setStatus(StockIssueRequestItemStatus status) { this.status = status; }

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
