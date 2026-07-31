package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.GenderRestriction;

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
 * Campus Infrastructure hierarchy, level 2 — belongs to exactly one {@link Block}.
 * {@code floorNumber} drives display ordering (Ground = 0, 1st = 1, ...) — it is *not* what decides
 * whether the Campus Setup skyline diagram draws this floor above or below the ground line;
 * {@code isBasement} is the explicit flag for that, since ordering and physical position aren't
 * always the same thing an admin has numbered consistently. {@code isHostel}/
 * {@code genderRestriction} let an admin mark this floor as hostel space with a gender — setting
 * either cascades the same value down to every {@link Zone} underneath.
 */
@Entity
@Table(name = "floors")
@EntityListeners(AuditingEntityListener.class)
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(nullable = false)
    private String name;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(name = "is_hostel", nullable = false)
    private Boolean isHostel = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_restriction", length = 20)
    private GenderRestriction genderRestriction;

    @Column(name = "is_basement", nullable = false)
    private Boolean isBasement = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Floor() {}

    public Floor(Block block, String name, Integer floorNumber) {
        this.block = block;
        this.name = name;
        this.floorNumber = floorNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }

    public Boolean getIsHostel() { return isHostel; }
    public void setIsHostel(Boolean isHostel) { this.isHostel = isHostel; }

    public GenderRestriction getGenderRestriction() { return genderRestriction; }
    public void setGenderRestriction(GenderRestriction genderRestriction) { this.genderRestriction = genderRestriction; }

    public Boolean getIsBasement() { return isBasement; }
    public void setIsBasement(Boolean isBasement) { this.isBasement = isBasement; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
