package com.cms.inventory.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

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
 * A splittable breakdown of one {@link StockBalance} row's quantity across {@link InventoryBin}s
 * — a pure locator, not a second source of truth. {@code StockBalance.qtyOnHand} stays the
 * authoritative on-hand quantity; the sum of this bin's rows for a given {@code stockBalance}
 * should always equal it, but a balance may also have less than its full quantity allocated to
 * any bin (unbinned stock) — this table is only ever written to when a movement's caller supplies
 * a bin. Written only by {@code StockMovementService.recordMovement}, in the same transaction as
 * the {@link StockLedger}/{@link StockBalance} write it accompanies.
 */
@Entity
@Table(name = "stock_bin_allocations")
public class StockBinAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_balance_id", nullable = false)
    private StockBalance stockBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id", nullable = false)
    private InventoryBin bin;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StockBalance getStockBalance() { return stockBalance; }
    public void setStockBalance(StockBalance stockBalance) { this.stockBalance = stockBalance; }

    public InventoryBin getBin() { return bin; }
    public void setBin(InventoryBin bin) { this.bin = bin; }

    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
