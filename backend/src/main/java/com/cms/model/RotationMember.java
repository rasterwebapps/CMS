package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
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
 * One physical group of students (e.g. "Batch 1") rotating through a {@link RotationGroup}'s
 * slots. {@link #memberOrder} fixes this group's starting offset in the rotation cycle. Which
 * existing per-subject {@link Batch} represents this group at each slot is recorded in
 * {@link RotationMemberAssignment} — this entity carries no roster of its own.
 */
@Entity
@Table(name = "rotation_members")
@EntityListeners(AuditingEntityListener.class)
public class RotationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_group_id", nullable = false)
    private RotationGroup rotationGroup;

    @Column(name = "member_order", nullable = false)
    private Integer memberOrder;

    @Column(nullable = false, length = 100)
    private String label;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RotationMember() {
    }

    public RotationMember(RotationGroup rotationGroup, Integer memberOrder, String label) {
        this.rotationGroup = rotationGroup;
        this.memberOrder = memberOrder;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RotationGroup getRotationGroup() {
        return rotationGroup;
    }

    public void setRotationGroup(RotationGroup rotationGroup) {
        this.rotationGroup = rotationGroup;
    }

    public Integer getMemberOrder() {
        return memberOrder;
    }

    public void setMemberOrder(Integer memberOrder) {
        this.memberOrder = memberOrder;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
