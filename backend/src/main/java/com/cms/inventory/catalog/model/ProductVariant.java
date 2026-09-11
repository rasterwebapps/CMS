package com.cms.inventory.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.catalog.model.enums.StockTrackingMode;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * One sellable/stockable SKU of a {@link Product} (e.g. "Red / Large" vs "Blue / Small" of the
 * same base product) — the "parent/child SKU matrix." Deliberately carries its own {@code
 * trackingMode}/{@code standardCost}/{@code listPrice}/{@code barcode}/{@code attributeValues}
 * rather than resolving them from the parent at read time: a new variant's create form pre-fills
 * these from the parent {@link Product}'s current values (copy-at-creation), then they're
 * independently editable and never reference the parent again — same "snapshot, never re-derived"
 * posture as {@link ProductUomChainVersion} and {@code UomConversionTemplate}. Has no {@code
 * baseUom} of its own: a variant shares its parent's base unit and {@code ProductUomChainVersion}
 * (both are keyed off {@code product.baseUom}/{@code product.id}, which a variant never
 * overrides) — see the 2026-09-11 "ProductVariant" decision-log entry for why that needs no
 * separate schema. {@code category}/{@code brand}/{@code hsnSacCode}/{@code defaultTaxRuleId}/
 * dimensions are likewise always the parent's; a variant doesn't override them.
 */
@Entity
@Table(name = "product_variants")
@EntityListeners(AuditingEntityListener.class)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "variant_code", nullable = false, length = 50)
    private String variantCode;

    @Column(name = "variant_name", nullable = false, length = 200)
    private String variantName;

    @Column(length = 64)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_mode", nullable = false, length = 20)
    private StockTrackingMode trackingMode = StockTrackingMode.NONE;

    @Column(name = "standard_cost", precision = 14, scale = 2)
    private BigDecimal standardCost;

    @Column(name = "list_price", precision = 14, scale = 2)
    private BigDecimal listPrice;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductVariantAttributeValue> attributeValues = new ArrayList<>();

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

    public String getVariantCode() { return variantCode; }
    public void setVariantCode(String variantCode) { this.variantCode = variantCode; }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public StockTrackingMode getTrackingMode() { return trackingMode; }
    public void setTrackingMode(StockTrackingMode trackingMode) { this.trackingMode = trackingMode; }

    public BigDecimal getStandardCost() { return standardCost; }
    public void setStandardCost(BigDecimal standardCost) { this.standardCost = standardCost; }

    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<ProductVariantAttributeValue> getAttributeValues() { return attributeValues; }
    public void setAttributeValues(List<ProductVariantAttributeValue> attributeValues) { this.attributeValues = attributeValues; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
