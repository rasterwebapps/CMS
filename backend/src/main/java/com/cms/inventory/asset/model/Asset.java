package com.cms.inventory.asset.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.asset.model.enums.AssetStatus;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.receiving.model.GoodsReceiptLine;
import com.cms.inventory.stock.model.InventoryLocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * Phase 5's ("Equipment & Asset Management") first slice — one physical, individually-tracked
 * unit of a {@link Product} (a specific laptop, a specific microscope), distinct from the
 * catalog/stock-ledger's aggregate quantity tracking. {@code goodsReceiptLine} is an optional
 * traceability link back to the specific delivery this unit came from, for an asset onboarded
 * through the normal procure→receive flow; a standalone "already-owned, being onboarded" asset
 * (pre-dating this module, or acquired outside it) simply leaves it {@code null}. {@code
 * purchaseValue}/{@code purchaseDate}/{@code usefulLifeMonths}/{@code salvageValue} are captured
 * now as asset master data even though nothing computes depreciation from them yet — that's the
 * "Depreciation slice" still to come. See the "Asset register slice" decision-log entry.
 */
@Entity
@Table(name = "assets")
@EntityListeners(AuditingEntityListener.class)
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(name = "asset_tag", nullable = false, length = 50)
    private String assetTag;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_line_id")
    private GoodsReceiptLine goodsReceiptLine;

    @Column(name = "purchase_value", precision = 14, scale = 2)
    private BigDecimal purchaseValue;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    @Column(name = "salvage_value", precision = 14, scale = 2)
    private BigDecimal salvageValue;

    @Column(name = "disposal_reason", length = 500)
    private String disposalReason;

    @Column(name = "disposal_value", precision = 14, scale = 2)
    private BigDecimal disposalValue;

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "disposed_by", length = 255)
    private String disposedBy;

    @Column(name = "disposed_at")
    private Instant disposedAt;

    @Column(length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }

    public GoodsReceiptLine getGoodsReceiptLine() { return goodsReceiptLine; }
    public void setGoodsReceiptLine(GoodsReceiptLine goodsReceiptLine) { this.goodsReceiptLine = goodsReceiptLine; }

    public BigDecimal getPurchaseValue() { return purchaseValue; }
    public void setPurchaseValue(BigDecimal purchaseValue) { this.purchaseValue = purchaseValue; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public Integer getUsefulLifeMonths() { return usefulLifeMonths; }
    public void setUsefulLifeMonths(Integer usefulLifeMonths) { this.usefulLifeMonths = usefulLifeMonths; }

    public BigDecimal getSalvageValue() { return salvageValue; }
    public void setSalvageValue(BigDecimal salvageValue) { this.salvageValue = salvageValue; }

    public String getDisposalReason() { return disposalReason; }
    public void setDisposalReason(String disposalReason) { this.disposalReason = disposalReason; }

    public BigDecimal getDisposalValue() { return disposalValue; }
    public void setDisposalValue(BigDecimal disposalValue) { this.disposalValue = disposalValue; }

    public LocalDate getDisposalDate() { return disposalDate; }
    public void setDisposalDate(LocalDate disposalDate) { this.disposalDate = disposalDate; }

    public String getDisposedBy() { return disposedBy; }
    public void setDisposedBy(String disposedBy) { this.disposedBy = disposedBy; }

    public Instant getDisposedAt() { return disposedAt; }
    public void setDisposedAt(Instant disposedAt) { this.disposedAt = disposedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
