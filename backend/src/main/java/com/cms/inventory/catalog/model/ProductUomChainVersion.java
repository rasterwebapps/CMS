package com.cms.inventory.catalog.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
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
 * One version of a {@link Product}'s unit-of-measure chain (e.g. Tablet -> Strip -> Box ->
 * Carton, each level's factor stored on its {@link ProductUomLevel} row). A pack-size change
 * never edits an existing version's levels — it creates a new version, or reactivates a prior
 * one that already matches (a repack reverting from 15s back to 10s), and flips {@code isActive}.
 * Only one version is active per product at a time (see the DB's partial unique index). Design:
 * the "Unit-of-Measure Hierarchy slice" decision-log entry (2026-09-10).
 */
@Entity
@Table(name = "product_uom_chain_versions")
@EntityListeners(AuditingEntityListener.class)
public class ProductUomChainVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "created_by")
    private String createdBy;

    @OneToMany(mappedBy = "chainVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("levelRank ASC")
    private List<ProductUomLevel> levels = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<ProductUomLevel> getLevels() { return levels; }
    public void setLevels(List<ProductUomLevel> levels) { this.levels = levels; }

    public Instant getCreatedAt() { return createdAt; }
}
