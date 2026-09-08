package com.cms.inventory.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.stock.model.enums.CycleCountLineStatus;

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
 * One product's row on a {@link CycleCount}'s count sheet. {@code systemQtySnapshot} is fixed the
 * moment this line is created (see the migration's column comment) and never re-fetched — the
 * counted-vs-system comparison is against that fixed baseline, not a live balance. {@code
 * ledgerRefId} is a loose reference (a plain id, no FK relation) to the {@code StockLedger} row an
 * approved variance posts, the same style {@link StockLedger#getRefId()} itself uses elsewhere.
 */
@Entity
@Table(name = "cycle_count_lines")
public class CycleCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_count_id", nullable = false)
    private CycleCount cycleCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "system_qty_snapshot", nullable = false, precision = 14, scale = 3)
    private BigDecimal systemQtySnapshot;

    @Column(name = "counted_qty", precision = 14, scale = 3)
    private BigDecimal countedQty;

    @Column(name = "variance_qty", precision = 14, scale = 3)
    private BigDecimal varianceQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleCountLineStatus status = CycleCountLineStatus.PENDING_COUNT;

    @Column(name = "counted_by", length = 255)
    private String countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "ledger_ref_id")
    private Long ledgerRefId;

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CycleCount getCycleCount() { return cycleCount; }
    public void setCycleCount(CycleCount cycleCount) { this.cycleCount = cycleCount; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getSystemQtySnapshot() { return systemQtySnapshot; }
    public void setSystemQtySnapshot(BigDecimal systemQtySnapshot) { this.systemQtySnapshot = systemQtySnapshot; }

    public BigDecimal getCountedQty() { return countedQty; }
    public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }

    public BigDecimal getVarianceQty() { return varianceQty; }
    public void setVarianceQty(BigDecimal varianceQty) { this.varianceQty = varianceQty; }

    public CycleCountLineStatus getStatus() { return status; }
    public void setStatus(CycleCountLineStatus status) { this.status = status; }

    public String getCountedBy() { return countedBy; }
    public void setCountedBy(String countedBy) { this.countedBy = countedBy; }

    public Instant getCountedAt() { return countedAt; }
    public void setCountedAt(Instant countedAt) { this.countedAt = countedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Long getLedgerRefId() { return ledgerRefId; }
    public void setLedgerRefId(Long ledgerRefId) { this.ledgerRefId = ledgerRefId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
