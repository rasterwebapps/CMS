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
 * Which existing per-subject {@link Batch} represents a {@link RotationMember} when its turn
 * lands on a given {@link RotationSlot} — reuses that Batch's own roster/capacity/venue
 * entirely. E.g. "Batch 1" is represented by the "English Batch 1" Batch at the English slot and
 * by the "Tamil Batch 1" Batch at the Tamil slot.
 */
@Entity
@Table(name = "rotation_member_assignments")
@EntityListeners(AuditingEntityListener.class)
public class RotationMemberAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_member_id", nullable = false)
    private RotationMember rotationMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_slot_id", nullable = false)
    private RotationSlot rotationSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RotationMemberAssignment() {
    }

    public RotationMemberAssignment(RotationMember rotationMember, RotationSlot rotationSlot, Batch batch) {
        this.rotationMember = rotationMember;
        this.rotationSlot = rotationSlot;
        this.batch = batch;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RotationMember getRotationMember() {
        return rotationMember;
    }

    public void setRotationMember(RotationMember rotationMember) {
        this.rotationMember = rotationMember;
    }

    public RotationSlot getRotationSlot() {
        return rotationSlot;
    }

    public void setRotationSlot(RotationSlot rotationSlot) {
        this.rotationSlot = rotationSlot;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
