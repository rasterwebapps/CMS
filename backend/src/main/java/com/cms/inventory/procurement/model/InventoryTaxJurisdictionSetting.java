package com.cms.inventory.procurement.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Singleton row (id pinned to 1 by a DB CHECK) holding the institution's own state, compared
 * against a supplier's state to resolve {@code JurisdictionMode} on each PO line. Starts absent
 * (zero rows) rather than seeded with a placeholder — {@code JurisdictionService} surfaces "not
 * configured" until an admin sets it. See the "GAP-02 pickup" decision-log entry.
 */
@Entity
@Table(name = "inventory_tax_jurisdiction_settings")
public class InventoryTaxJurisdictionSetting {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "home_state", nullable = false, length = 100)
    private String homeState;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHomeState() { return homeState; }
    public void setHomeState(String homeState) { this.homeState = homeState; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
