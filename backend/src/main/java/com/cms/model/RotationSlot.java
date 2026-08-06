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
 * One {@link ClassSchedule} cell participating in a {@link RotationGroup} (e.g. "English Lab,
 * Wed P3-4"). Once linked here, {@link ClassSchedule#getBatch()} on the underlying row is null
 * — its occupant is resolved per-date by RotationResolverService instead of being fixed.
 * {@link #slotOrder} is this cell's fixed position in the rotation cycle.
 */
@Entity
@Table(name = "rotation_slots")
@EntityListeners(AuditingEntityListener.class)
public class RotationSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_group_id", nullable = false)
    private RotationGroup rotationGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(name = "slot_order", nullable = false)
    private Integer slotOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RotationSlot() {
    }

    public RotationSlot(RotationGroup rotationGroup, ClassSchedule classSchedule, Integer slotOrder) {
        this.rotationGroup = rotationGroup;
        this.classSchedule = classSchedule;
        this.slotOrder = slotOrder;
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

    public ClassSchedule getClassSchedule() {
        return classSchedule;
    }

    public void setClassSchedule(ClassSchedule classSchedule) {
        this.classSchedule = classSchedule;
    }

    public Integer getSlotOrder() {
        return slotOrder;
    }

    public void setSlotOrder(Integer slotOrder) {
        this.slotOrder = slotOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
