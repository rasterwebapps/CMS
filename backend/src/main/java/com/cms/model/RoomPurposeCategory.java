package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.RoomPurposeCategoryCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Primary Purpose Category for a {@link Room} (Academic, Residential, Administrative, etc.) — tier
 * one of the 2-tier Room Purpose Classification. {@code isResidential} is the authoritative flag
 * checked before a Room can be designated a {@link HostelRoom}, not a hardcoded {@code code} match,
 * so it survives an admin renaming/recoding this category. {@code code} itself is a fixed
 * {@link RoomPurposeCategoryCode} (picked from a list, immutable once set by
 * {@code RoomPurposeCategoryService}) rather than free text, for the same reason — anything that
 * keys off a specific category (e.g. "must be ACADEMIC") needs that identity to never shift.
 */
@Entity
@Table(name = "room_purpose_categories")
@EntityListeners(AuditingEntityListener.class)
public class RoomPurposeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoomPurposeCategoryCode code;

    @Column(name = "is_residential", nullable = false)
    private Boolean isResidential = false;

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

    public RoomPurposeCategory() {}

    public RoomPurposeCategory(String name, RoomPurposeCategoryCode code, Boolean isResidential, String description) {
        this.name = name;
        this.code = code;
        this.isResidential = isResidential != null && isResidential;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoomPurposeCategoryCode getCode() { return code; }
    public void setCode(RoomPurposeCategoryCode code) { this.code = code; }

    public Boolean getIsResidential() { return isResidential; }
    public void setIsResidential(Boolean isResidential) { this.isResidential = isResidential; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
