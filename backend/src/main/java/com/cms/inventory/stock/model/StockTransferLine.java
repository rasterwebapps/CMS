package com.cms.inventory.stock.model;

import java.math.BigDecimal;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One product's row on a {@link StockTransfer}. No batch/unit-cost fields — see the class docs on {@link StockTransfer}. */
@Entity
@Table(name = "stock_transfer_lines")
public class StockTransferLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Required once {@link #product} has any active variant — see {@code
     *  StockTransferService.resolveVariant}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(length = 500)
    private String notes;

    /** Optional — the bin this line's quantity is decreased from, within the transfer's source
     *  location. Null means the source leg stays unbinned. See {@code StockBinAllocation}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_bin_id")
    private InventoryBin sourceBin;

    /** Optional — the bin this line's quantity is increased into, within the transfer's
     *  destination location. Null means the destination leg stays unbinned. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_bin_id")
    private InventoryBin destinationBin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StockTransfer getStockTransfer() { return stockTransfer; }
    public void setStockTransfer(StockTransfer stockTransfer) { this.stockTransfer = stockTransfer; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public InventoryBin getSourceBin() { return sourceBin; }
    public void setSourceBin(InventoryBin sourceBin) { this.sourceBin = sourceBin; }

    public InventoryBin getDestinationBin() { return destinationBin; }
    public void setDestinationBin(InventoryBin destinationBin) { this.destinationBin = destinationBin; }
}
