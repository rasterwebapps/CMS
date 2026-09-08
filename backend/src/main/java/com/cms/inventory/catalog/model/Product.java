package com.cms.inventory.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * An item in the Inventory catalog — the thing that gets purchased, stocked, issued, or tracked
 * as an asset, depending on its {@code isAsset}/{@code isConsumable}/{@code isService}/
 * {@code isLoanable} flags (not mutually exclusive; a deployment decides what combination makes
 * sense for a given product). {@code aliases} and {@code attributeValues} are managed as child
 * collections replaced wholesale on every save — see the 2026-09-07 "Product slice" decision-log
 * entry. {@code ProductImage} is deliberately not modelled yet (same entry).
 * Design: docs/inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md §2.
 */
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_uom_id", nullable = false)
    private Uom baseUom;

    @Column(name = "reorder_level", precision = 14, scale = 3)
    private BigDecimal reorderLevel;

    @Column(name = "reorder_qty", precision = 14, scale = 3)
    private BigDecimal reorderQty;

    @Column(name = "is_asset", nullable = false)
    private Boolean isAsset = false;

    @Column(name = "is_consumable", nullable = false)
    private Boolean isConsumable = true;

    @Column(name = "is_service", nullable = false)
    private Boolean isService = false;

    @Column(name = "is_loanable", nullable = false)
    private Boolean isLoanable = false;

    @Column(name = "depreciation_rate", precision = 5, scale = 2)
    private BigDecimal depreciationRate;

    @Column(name = "warranty_period_months")
    private Integer warrantyPeriodMonths;

    @Column(length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ProductAlias> aliases = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Uom getBaseUom() { return baseUom; }
    public void setBaseUom(Uom baseUom) { this.baseUom = baseUom; }

    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }

    public BigDecimal getReorderQty() { return reorderQty; }
    public void setReorderQty(BigDecimal reorderQty) { this.reorderQty = reorderQty; }

    public Boolean getIsAsset() { return isAsset; }
    public void setIsAsset(Boolean isAsset) { this.isAsset = isAsset; }

    public Boolean getIsConsumable() { return isConsumable; }
    public void setIsConsumable(Boolean isConsumable) { this.isConsumable = isConsumable; }

    public Boolean getIsService() { return isService; }
    public void setIsService(Boolean isService) { this.isService = isService; }

    public Boolean getIsLoanable() { return isLoanable; }
    public void setIsLoanable(Boolean isLoanable) { this.isLoanable = isLoanable; }

    public BigDecimal getDepreciationRate() { return depreciationRate; }
    public void setDepreciationRate(BigDecimal depreciationRate) { this.depreciationRate = depreciationRate; }

    public Integer getWarrantyPeriodMonths() { return warrantyPeriodMonths; }
    public void setWarrantyPeriodMonths(Integer warrantyPeriodMonths) { this.warrantyPeriodMonths = warrantyPeriodMonths; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<ProductAlias> getAliases() { return aliases; }
    public void setAliases(List<ProductAlias> aliases) { this.aliases = aliases; }

    public List<ProductAttributeValue> getAttributeValues() { return attributeValues; }
    public void setAttributeValues(List<ProductAttributeValue> attributeValues) { this.attributeValues = attributeValues; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
