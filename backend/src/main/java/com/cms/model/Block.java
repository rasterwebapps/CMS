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
 * Campus Infrastructure hierarchy, level 2 of Organization &gt; Branch &gt; Block &gt; Floor &gt;
 * Zone &gt; Room — belongs to exactly one {@link Branch}. Generic/shared physical structure, not
 * hostel-specific. {@code isHostel}/{@code genderRestriction} let an admin mark an entire block as
 * hostel space with a gender — setting either cascades the same value down to every {@link Floor}
 * and {@link Zone} underneath (see {@code CampusInfrastructureService}).
 */
@Entity
@Table(name = "blocks")
@EntityListeners(AuditingEntityListener.class)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(name = "is_hostel", nullable = false)
    private Boolean isHostel = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_restriction", length = 20)
    private GenderRestriction genderRestriction;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Block() {}

    public Block(Branch branch, String name, String code, String description) {
        this.branch = branch;
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsHostel() { return isHostel; }
    public void setIsHostel(Boolean isHostel) { this.isHostel = isHostel; }

    public GenderRestriction getGenderRestriction() { return genderRestriction; }
    public void setGenderRestriction(GenderRestriction genderRestriction) { this.genderRestriction = genderRestriction; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
