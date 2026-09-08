package com.cms.inventory.stock.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.stock.model.enums.LocationRole;
import com.cms.model.Room;

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
 * A stock-keeping location Inventory uses — wraps an existing Infra {@link Room} rather than
 * duplicating the campus location hierarchy, with a virtual/display name and a role (Store vs.
 * Requesting Point) layered on top. Room-only in this pass — no Zone-level locations yet; see the
 * 2026-09-07 "Stock Tracking slice" decision-log entry for why, and before adding Zone support.
 * Multiple InventoryLocations may point at the same Room (e.g. two Labs sharing a physical room),
 * so {@code virtualName} — not {@code room} — is the unique, user-facing identity.
 */
@Entity
@Table(name = "inventory_locations")
@EntityListeners(AuditingEntityListener.class)
public class InventoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "virtual_name", nullable = false, length = 150)
    private String virtualName;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_role", nullable = false, length = 20)
    private LocationRole locationRole;

    @Column(length = 500)
    private String description;

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

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getVirtualName() { return virtualName; }
    public void setVirtualName(String virtualName) { this.virtualName = virtualName; }

    public LocationRole getLocationRole() { return locationRole; }
    public void setLocationRole(LocationRole locationRole) { this.locationRole = locationRole; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
