package com.cms.inventory.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.inventory.catalog.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Materialized/derived current on-hand quantity and value for one Product+Location(+Batch) —
 * kept in sync by {@code StockMovementService} on every {@link StockLedger} write, per the
 * append-only-ledger-with-a-derived-balance-table decision recorded in `DECISION_LOG.md`. Never
 * written to directly by user-facing code; treat this entity as read-only outside that service.
 */
@Entity
@Table(name = "stock_balances")
public class StockBalance {

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

    @Column(name = "qty_on_hand", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyOnHand = BigDecimal.ZERO;

    @Column(name = "value_on_hand", nullable = false, precision = 14, scale = 2)
    private BigDecimal valueOnHand = BigDecimal.ZERO;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public StockBatch getBatch() { return batch; }
    public void setBatch(StockBatch batch) { this.batch = batch; }

    public BigDecimal getQtyOnHand() { return qtyOnHand; }
    public void setQtyOnHand(BigDecimal qtyOnHand) { this.qtyOnHand = qtyOnHand; }

    public BigDecimal getValueOnHand() { return valueOnHand; }
    public void setValueOnHand(BigDecimal valueOnHand) { this.valueOnHand = valueOnHand; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
