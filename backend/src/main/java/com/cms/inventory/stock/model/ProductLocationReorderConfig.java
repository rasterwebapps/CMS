package com.cms.inventory.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.catalog.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Phase B of the Stock Indent auto-indent feature (see the "OC-206 reopened" decision-log entry,
 * 2026-09-21) — a {@link Product}'s reorder level, reorder quantity, and max stock quantity at one
 * specific {@link InventoryLocation}, plus whether a breach should auto-generate a Stock Indent.
 * Deliberately per-(product, location), not a reuse of {@code Product.reorderLevel}/{@code
 * reorderQty} — those stay global and keep feeding the separate Wanted List's supplier-side
 * auto-reorder job; this is a different signal (an internal transfer request), scoped this phase
 * to {@code REQUESTING_POINT}/{@code BOTH} locations only (enforced in {@code
 * ProductLocationReorderConfigService}, not here — a {@code STORE} running low stays the Wanted
 * List's job). At most one active config per (product, location) pair (partial unique index,
 * V533) — same shape as {@code VendorProductMapping}'s (supplier, product) uniqueness.
 */
@Entity
@Table(name = "product_location_reorder_configs")
@EntityListeners(AuditingEntityListener.class)
public class ProductLocationReorderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(name = "reorder_level", nullable = false, precision = 14, scale = 3)
    private BigDecimal reorderLevel;

    /** The lot size auto-indented once {@code reorderLevel} is breached — same "fixed reorder lot"
     *  role as {@code Product.reorderQty} plays for the Wanted List. */
    @Column(name = "reorder_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal reorderQty;

    /** Optional ceiling an auto-generated indent's suggested quantity won't push this location's
     *  stock past. {@code null} means no cap beyond {@code reorderQty} itself. */
    @Column(name = "max_stock_qty", precision = 14, scale = 3)
    private BigDecimal maxStockQty;

    @Column(name = "auto_indent_enabled", nullable = false)
    private Boolean autoIndentEnabled = true;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }

    public BigDecimal getReorderQty() { return reorderQty; }
    public void setReorderQty(BigDecimal reorderQty) { this.reorderQty = reorderQty; }

    public BigDecimal getMaxStockQty() { return maxStockQty; }
    public void setMaxStockQty(BigDecimal maxStockQty) { this.maxStockQty = maxStockQty; }

    public Boolean getAutoIndentEnabled() { return autoIndentEnabled; }
    public void setAutoIndentEnabled(Boolean autoIndentEnabled) { this.autoIndentEnabled = autoIndentEnabled; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
