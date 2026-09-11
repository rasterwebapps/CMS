package com.cms.inventory.catalog.model;

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
 * A reusable, named unit-of-measure conversion chain (e.g. "Pharma Tablet Pack": Tablet -> Strip
 * (x10) -> Box (x100)) a {@link Product}'s own Unit Hierarchy can be pre-filled from instead of
 * typed out each time — see the 2026-09-11 "Shared/global UOM conversion templates" decision-log
 * entry. Deliberately no live link once applied: a product's {@link ProductUomChainVersion}/
 * {@link ProductUomLevel} rows are saved as their own independent snapshot the same way every
 * other chain save already works, and a later edit to this template never reaches back into any
 * product that used it. {@code levels} is a child collection replaced wholesale on every save,
 * same pattern as {@code Product.aliases}/{@code attributeValues}. {@code baseUom} must equal
 * {@code levels}' own level-0 entry's unit (enforced in {@code UomConversionTemplateService}, not
 * a DB constraint) — stored redundantly on the template itself so "which templates apply to a
 * product with base unit X" can be queried directly, without loading every template's levels.
 */
@Entity
@Table(name = "uom_conversion_templates")
@EntityListeners(AuditingEntityListener.class)
public class UomConversionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_uom_id", nullable = false)
    private Uom baseUom;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("levelRank ASC")
    private List<UomConversionTemplateLevel> levels = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Uom getBaseUom() { return baseUom; }
    public void setBaseUom(Uom baseUom) { this.baseUom = baseUom; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<UomConversionTemplateLevel> getLevels() { return levels; }
    public void setLevels(List<UomConversionTemplateLevel> levels) { this.levels = levels; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
