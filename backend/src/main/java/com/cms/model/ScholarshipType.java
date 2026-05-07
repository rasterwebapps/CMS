package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.DiscountType;
import com.cms.model.enums.ScholarshipApplicationMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scholarship_types")
@EntityListeners(AuditingEntityListener.class)
public class ScholarshipType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "govt_scheme", nullable = false)
    private boolean govtScheme;

    @Column(name = "scheme_code", length = 50)
    private String schemeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_amount_per_year", precision = 12, scale = 2)
    private BigDecimal maxAmountPerYear;

    @Column(name = "renewal_required", nullable = false)
    private boolean renewalRequired;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * How this scholarship is processed: internally by the institution, or externally
     * via a government portal (NSP, ePass TN, TNSMS). Defaults to INSTITUTION.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "application_mode", nullable = false, length = 20)
    private ScholarshipApplicationMode applicationMode = ScholarshipApplicationMode.INSTITUTION;

    /** Display name of the govt portal (e.g. "NSP", "ePass TN"). Null for institution mode. */
    @Column(name = "portal_name", length = 50)
    private String portalName;

    /** URL of the govt portal where application is submitted. Null for institution mode. */
    @Column(name = "portal_url", length = 255)
    private String portalUrl;

    /** Earliest year of study eligible (1 = 1st year). Null = no lower bound. */
    @Column(name = "eligible_from_year")
    private Integer eligibleFromYear;

    /** Latest year of study eligible. Null = no upper bound. */
    @Column(name = "eligible_to_year")
    private Integer eligibleToYear;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isGovtScheme() { return govtScheme; }
    public void setGovtScheme(boolean govtScheme) { this.govtScheme = govtScheme; }
    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMaxAmountPerYear() { return maxAmountPerYear; }
    public void setMaxAmountPerYear(BigDecimal maxAmountPerYear) { this.maxAmountPerYear = maxAmountPerYear; }
    public boolean isRenewalRequired() { return renewalRequired; }
    public void setRenewalRequired(boolean renewalRequired) { this.renewalRequired = renewalRequired; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public ScholarshipApplicationMode getApplicationMode() { return applicationMode; }
    public void setApplicationMode(ScholarshipApplicationMode applicationMode) {
        this.applicationMode = applicationMode == null ? ScholarshipApplicationMode.INSTITUTION : applicationMode;
    }
    public String getPortalName() { return portalName; }
    public void setPortalName(String portalName) { this.portalName = portalName; }
    public String getPortalUrl() { return portalUrl; }
    public void setPortalUrl(String portalUrl) { this.portalUrl = portalUrl; }
    public Integer getEligibleFromYear() { return eligibleFromYear; }
    public void setEligibleFromYear(Integer eligibleFromYear) { this.eligibleFromYear = eligibleFromYear; }
    public Integer getEligibleToYear() { return eligibleToYear; }
    public void setEligibleToYear(Integer eligibleToYear) { this.eligibleToYear = eligibleToYear; }
}

