package com.cms.inventory.procurement.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Singleton row (id pinned to 1 by a DB CHECK) holding the institution's own base currency —
 * every {@code CurrencyExchangeRate} converts *to* this currency, and {@code
 * VendorProductMappingService} uses it to resolve a mapping's price into base-currency terms.
 * Starts absent (zero rows) rather than seeded with a placeholder, same "not configured until an
 * admin sets it" posture as {@link InventoryTaxJurisdictionSetting}. See the 2026-09-11
 * "Multi-currency FX" decision-log entry.
 */
@Entity
@Table(name = "inventory_currency_settings")
public class InventoryCurrencySetting {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBaseCurrencyCode() { return baseCurrencyCode; }
    public void setBaseCurrencyCode(String baseCurrencyCode) { this.baseCurrencyCode = baseCurrencyCode; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
