package com.cms.inventory.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
 * One level of a {@link ProductUomChainVersion} (e.g. "Strip", factor 10 -> the chain's base
 * unit). {@code levelRank} 0 is always the chain's base level (must equal the owning {@link
 * Product}'s {@code baseUom}, {@code factorToBase} = 1) — enforced in {@code
 * ProductUomChainService}, not a DB constraint. {@code factorToBase} multiplies directly to the
 * base unit (not to the next level down) so correcting one level never silently reshapes the
 * levels above it. {@code isDefaultPurchase} marks which level a new Purchase Order line defaults
 * to — still changeable per line to any other level in the active chain.
 */
@Entity
@Table(name = "product_uom_levels")
@EntityListeners(AuditingEntityListener.class)
public class ProductUomLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_version_id", nullable = false)
    private ProductUomChainVersion chainVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private Uom uom;

    @Column(name = "level_rank", nullable = false)
    private Integer levelRank;

    @Column(name = "factor_to_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal factorToBase = BigDecimal.ONE;

    @Column(name = "is_default_purchase", nullable = false)
    private Boolean isDefaultPurchase = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductUomChainVersion getChainVersion() { return chainVersion; }
    public void setChainVersion(ProductUomChainVersion chainVersion) { this.chainVersion = chainVersion; }

    public Uom getUom() { return uom; }
    public void setUom(Uom uom) { this.uom = uom; }

    public Integer getLevelRank() { return levelRank; }
    public void setLevelRank(Integer levelRank) { this.levelRank = levelRank; }

    public BigDecimal getFactorToBase() { return factorToBase; }
    public void setFactorToBase(BigDecimal factorToBase) { this.factorToBase = factorToBase; }

    public Boolean getIsDefaultPurchase() { return isDefaultPurchase; }
    public void setIsDefaultPurchase(Boolean isDefaultPurchase) { this.isDefaultPurchase = isDefaultPurchase; }

    public Instant getCreatedAt() { return createdAt; }
}
