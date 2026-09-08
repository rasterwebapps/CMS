package com.cms.inventory.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.stock.model.enums.StockTxnType;

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
 * The append-only source of truth for every stock movement — never updated or deleted once
 * written, only appended (hence no {@code updatedAt}/auditing listener; {@code txnDate} is set
 * explicitly by the writing service). {@link StockBalance} is a materialized rollup kept in sync
 * by the same service on every write here, never written to directly.
 */
@Entity
@Table(name = "stock_ledger")
public class StockLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private StockBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 30)
    private StockTxnType txnType;

    /** Signed — positive for an increase (Receipt, a found-extra Adjustment), negative for a
     *  decrease (Disposal, a found-missing Adjustment). */
    @Column(name = "qty_delta", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyDelta;

    @Column(name = "unit_cost", precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(length = 500)
    private String notes;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "txn_date", nullable = false)
    private Instant txnDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public StockBatch getBatch() { return batch; }
    public void setBatch(StockBatch batch) { this.batch = batch; }

    public StockTxnType getTxnType() { return txnType; }
    public void setTxnType(StockTxnType txnType) { this.txnType = txnType; }

    public BigDecimal getQtyDelta() { return qtyDelta; }
    public void setQtyDelta(BigDecimal qtyDelta) { this.qtyDelta = qtyDelta; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public Instant getTxnDate() { return txnDate; }
    public void setTxnDate(Instant txnDate) { this.txnDate = txnDate; }
}
