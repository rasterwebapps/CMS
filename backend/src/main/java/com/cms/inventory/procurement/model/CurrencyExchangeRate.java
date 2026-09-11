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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A manually-entered rate converting {@code currencyCode} to the institution's base currency
 * ({@link InventoryCurrencySetting}), as of {@code effectiveDate}. Multiple dated rows per
 * currency are allowed — rates change over time — with the most recent row whose {@code
 * effectiveDate} is on or before "today" being the one in effect, resolved at read time in
 * {@code CurrencyExchangeRateService}. No live FX feed: this is a reference table an admin keeps
 * current by hand, same "manual entry, no revaluation engine" posture {@code
 * PurchaseOrder.currencyCode}/{@code exchangeRate} already commit to. See the 2026-09-11
 * "Multi-currency FX" decision-log entry.
 */
@Entity
@Table(name = "currency_exchange_rates")
@EntityListeners(AuditingEntityListener.class)
public class CurrencyExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "rate_to_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal rateToBase;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

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

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getRateToBase() { return rateToBase; }
    public void setRateToBase(BigDecimal rateToBase) { this.rateToBase = rateToBase; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
