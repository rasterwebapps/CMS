package com.cms.inventory.catalog.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * The typed-EAV value a {@link ProductVariant} carries for one of its parent's Category's {@link
 * CategoryAttribute} definitions — same shape as {@link ProductAttributeValue}, just keyed to a
 * variant instead of a product directly (e.g. "Size" = "Large" on a specific variant, while
 * non-variant-defining attributes are simply not overridden here and read from the parent
 * product's own values instead — see {@code ProductVariantService}).
 */
@Entity
@Table(name = "product_variant_attribute_values")
public class ProductVariantAttributeValue implements TypedAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private CategoryAttribute attribute;

    @Column(name = "text_value", length = 500)
    private String textValue;

    @Column(name = "number_value", precision = 18, scale = 4)
    private BigDecimal numberValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public CategoryAttribute getAttribute() { return attribute; }
    public void setAttribute(CategoryAttribute attribute) { this.attribute = attribute; }

    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }

    public BigDecimal getNumberValue() { return numberValue; }
    public void setNumberValue(BigDecimal numberValue) { this.numberValue = numberValue; }

    public LocalDate getDateValue() { return dateValue; }
    public void setDateValue(LocalDate dateValue) { this.dateValue = dateValue; }

    public Boolean getBooleanValue() { return booleanValue; }
    public void setBooleanValue(Boolean booleanValue) { this.booleanValue = booleanValue; }
}
