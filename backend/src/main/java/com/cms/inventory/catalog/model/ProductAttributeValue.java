package com.cms.inventory.catalog.model;

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
 * collection of its Product, replaced wholesale on every Product save. Stored as plain text
 * regardless of the attribute's declared data type — parsing/validating by type is a display/
 * input-form concern, not a storage concern.
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

    @Column(length = 500)
    private String value;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public CategoryAttribute getAttribute() { return attribute; }
    public void setAttribute(CategoryAttribute attribute) { this.attribute = attribute; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
