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
 * Campus Infrastructure hierarchy, level 3 — belongs to exactly one {@link Floor}. Leaf level for
 * the hostel/gender cascade (Room has no such field): {@code isHostel}/{@code genderRestriction}
 * can be set directly here even if the parent Floor/Block are not hostel-marked, covering a single
 * hostel wing inside an otherwise non-hostel floor/block. {@code genderRestriction} null means
 * unrestricted/mixed, not "unset." {@code warden} is optional and independent per zone, so a mixed
 * floor can have separate wardens per wing.
 */
@Entity
@Table(name = "zones")
@EntityListeners(AuditingEntityListener.class)
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_hostel", nullable = false)
    private Boolean isHostel = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_restriction", length = 20)
    private GenderRestriction genderRestriction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warden_id")
    private Faculty warden;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Display order among sibling Zones on the same Floor — drives drag-to-reorder in the Campus
     *  Setup skyline. Not gap-free after deletions; only relative order matters. */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Zone() {}

    public Zone(Floor floor, String name, GenderRestriction genderRestriction, Faculty warden) {
        this.floor = floor;
        this.name = name;
        this.genderRestriction = genderRestriction;
        this.warden = warden;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Floor getFloor() { return floor; }
    public void setFloor(Floor floor) { this.floor = floor; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsHostel() { return isHostel; }
    public void setIsHostel(Boolean isHostel) { this.isHostel = isHostel; }

    public GenderRestriction getGenderRestriction() { return genderRestriction; }
    public void setGenderRestriction(GenderRestriction genderRestriction) { this.genderRestriction = genderRestriction; }

    public Faculty getWarden() { return warden; }
    public void setWarden(Faculty warden) { this.warden = warden; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
