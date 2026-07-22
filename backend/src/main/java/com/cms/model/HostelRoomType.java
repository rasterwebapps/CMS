package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

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

@Entity
@Table(name = "hostel_room_types")
@EntityListeners(AuditingEntityListener.class)
public class HostelRoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "sharing_capacity", nullable = false)
    private Integer sharingCapacity;

    @Column(name = "is_ac", nullable = false)
    private Boolean isAc = false;

    @Column(name = "fee_amount_per_year", nullable = false)
    private BigDecimal feeAmountPerYear;

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

    public HostelRoomType() {}

    public HostelRoomType(String name, String code, Integer sharingCapacity, Boolean isAc,
                           BigDecimal feeAmountPerYear, String description) {
        this.name = name;
        this.code = code;
        this.sharingCapacity = sharingCapacity;
        this.isAc = isAc;
        this.feeAmountPerYear = feeAmountPerYear;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getSharingCapacity() { return sharingCapacity; }
    public void setSharingCapacity(Integer sharingCapacity) { this.sharingCapacity = sharingCapacity; }

    public Boolean getIsAc() { return isAc; }
    public void setIsAc(Boolean isAc) { this.isAc = isAc; }

    public BigDecimal getFeeAmountPerYear() { return feeAmountPerYear; }
    public void setFeeAmountPerYear(BigDecimal feeAmountPerYear) { this.feeAmountPerYear = feeAmountPerYear; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
