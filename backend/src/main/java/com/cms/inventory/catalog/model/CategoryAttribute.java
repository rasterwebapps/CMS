package com.cms.inventory.catalog.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.catalog.model.enums.AttributeDataType;

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
 * A custom attribute definition scoped to one {@link Category} (e.g. "Shelf Life" for a pharma
 * category, "Calibration Due" for a lab-instrument category) — the mechanism that lets different
 * kinds of items each carry the details relevant to them without hard-coding columns per
 * vertical. Edited only in the context of its owning Category; has no lifecycle of its own.
 * Design: docs/inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md §2.
 */
@Entity
@Table(name = "category_attributes")
@EntityListeners(AuditingEntityListener.class)
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeDataType dataType;

    /** Comma-separated allowed values — only meaningful when {@code dataType == ENUM}. */
    @Column(name = "enum_options", length = 1000)
    private String enumOptions;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AttributeDataType getDataType() { return dataType; }
    public void setDataType(AttributeDataType dataType) { this.dataType = dataType; }

    public String getEnumOptions() { return enumOptions; }
    public void setEnumOptions(String enumOptions) { this.enumOptions = enumOptions; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
