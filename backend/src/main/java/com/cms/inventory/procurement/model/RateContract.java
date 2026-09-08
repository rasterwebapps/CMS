package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
 * A standing agreement with a {@link Supplier} — a value cap, term text, and a renewal reminder
 * date. Given its own list+form screen and permission pair ({@code
 * INVENTORY_RATE_CONTRACT_VIEW}/{@code _MANAGE}), separate from {@code INVENTORY_SUPPLIER_*} —
 * negotiating/managing rate contracts is a genuinely distinct operation from managing a supplier's
 * master data, per the operation-wise permission mapping rule, even though both live under the
 * same "Purchasing & Suppliers" nav group. See the 2026-09-08 "Phase 2 kickoff" decision-log entry.
 */
@Entity
@Table(name = "rate_contracts")
@EntityListeners(AuditingEntityListener.class)
public class RateContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "contract_value_cap", precision = 14, scale = 2)
    private BigDecimal contractValueCap;

    @Column(name = "terms_text", length = 2000)
    private String termsText;

    @Column(name = "renewal_reminder_date")
    private LocalDate renewalReminderDate;

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

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getContractValueCap() { return contractValueCap; }
    public void setContractValueCap(BigDecimal contractValueCap) { this.contractValueCap = contractValueCap; }

    public String getTermsText() { return termsText; }
    public void setTermsText(String termsText) { this.termsText = termsText; }

    public LocalDate getRenewalReminderDate() { return renewalReminderDate; }
    public void setRenewalReminderDate(LocalDate renewalReminderDate) { this.renewalReminderDate = renewalReminderDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
