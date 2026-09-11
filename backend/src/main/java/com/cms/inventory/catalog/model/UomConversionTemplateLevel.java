package com.cms.inventory.catalog.model;

import java.math.BigDecimal;

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
 * One level of a {@link UomConversionTemplate} (e.g. "Strip", factor 10 -> the template's base
 * unit) — same shape as {@link ProductUomLevel}, just keyed to a reusable template instead of one
 * product's own chain version.
 */
@Entity
@Table(name = "uom_conversion_template_levels")
public class UomConversionTemplateLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private UomConversionTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private Uom uom;

    @Column(name = "level_rank", nullable = false)
    private Integer levelRank;

    @Column(name = "factor_to_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal factorToBase = BigDecimal.ONE;

    @Column(name = "is_default_purchase", nullable = false)
    private Boolean isDefaultPurchase = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UomConversionTemplate getTemplate() { return template; }
    public void setTemplate(UomConversionTemplate template) { this.template = template; }

    public Uom getUom() { return uom; }
    public void setUom(Uom uom) { this.uom = uom; }

    public Integer getLevelRank() { return levelRank; }
    public void setLevelRank(Integer levelRank) { this.levelRank = levelRank; }

    public BigDecimal getFactorToBase() { return factorToBase; }
    public void setFactorToBase(BigDecimal factorToBase) { this.factorToBase = factorToBase; }

    public Boolean getIsDefaultPurchase() { return isDefaultPurchase; }
    public void setIsDefaultPurchase(Boolean isDefaultPurchase) { this.isDefaultPurchase = isDefaultPurchase; }
}
