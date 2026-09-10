package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.procurement.model.enums.JurisdictionMode;

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
 * One component a {@link TaxRule}'s total rate fans out into for a given {@link JurisdictionMode}
 * — e.g. "GST 12%" fans out to a single IGST row at 100% under INTERSTATE, or CGST + SGST rows
 * splitting 100% between them under INTRASTATE. The sum of {@code splitPercent} across every row
 * sharing (taxRule, jurisdictionMode) must equal 100 — enforced in {@code TaxSubTypeService}
 * since a per-row CHECK can't express a cross-row sum. See the "GAP-02 pickup" decision-log entry.
 */
@Entity
@Table(name = "tax_sub_types")
@EntityListeners(AuditingEntityListener.class)
public class TaxSubType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rule_id", nullable = false)
    private TaxRule taxRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction_mode", nullable = false, length = 20)
    private JurisdictionMode jurisdictionMode;

    @Column(name = "component_name", nullable = false, length = 50)
    private String componentName;

    @Column(name = "split_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal splitPercent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TaxRule getTaxRule() { return taxRule; }
    public void setTaxRule(TaxRule taxRule) { this.taxRule = taxRule; }

    public JurisdictionMode getJurisdictionMode() { return jurisdictionMode; }
    public void setJurisdictionMode(JurisdictionMode jurisdictionMode) { this.jurisdictionMode = jurisdictionMode; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public BigDecimal getSplitPercent() { return splitPercent; }
    public void setSplitPercent(BigDecimal splitPercent) { this.splitPercent = splitPercent; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
