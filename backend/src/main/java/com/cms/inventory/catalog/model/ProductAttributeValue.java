package com.cms.inventory.catalog.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.inventory.catalog.model.enums.AttributeDataType;

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
 * The EAV-style value a {@link Product} carries for one of its Category's {@link CategoryAttribute}
 * definitions (e.g. "Shelf Life" = "24 months" for a chemical product). Managed as a child
 * collection of its Product, replaced wholesale on every Product save. Stored in the typed column
 * matching the attribute's declared {@link AttributeDataType} — exactly one of
 * {@code textValue}/{@code numberValue}/{@code dateValue}/{@code booleanValue} is ever populated
 * per row, chosen by {@link #getAttribute()}'s data type, not stored redundantly. Parsing the raw
 * form/API string into the right typed column, and validating it against the attribute's declared
 * type (and, for {@code ENUM}, its allowed options), is done by {@code ProductService} — this
 * entity only knows how to hold and render whichever typed value it was given.
 */
@Entity
@Table(name = "product_attribute_values")
public class ProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

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

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

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

    /** Renders whichever typed column is populated back to its plain-string form/API representation. */
    public String renderValue() {
        AttributeDataType type = attribute.getDataType();
        return switch (type) {
            case TEXT, ENUM -> textValue;
            case NUMBER -> numberValue == null ? null : numberValue.stripTrailingZeros().toPlainString();
            case DATE -> dateValue == null ? null : dateValue.toString();
            case BOOLEAN -> booleanValue == null ? null : booleanValue.toString();
        };
    }
}
