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
 * Which real {@link Faculty} represents a {@link RotationMember} when its turn comes up for a
 * clinical {@link Batch}'s escort duty (OC-175 Piece 3) — the escort-duty analog of
 * {@link RotationMemberAssignment}, but with the slot dimension collapsed (one escort duty per
 * batch, not interleaved across subjects) and the resolved payload a {@link Faculty} instead of a
 * {@link Batch}. See {@link com.cms.util.RotationParity} for the shared parity math and
 * {@code EscortRotationResolverService} for "whose turn is it on date X".
 */
@Entity
@Table(name = "escort_rotation_assignments")
@EntityListeners(AuditingEntityListener.class)
public class EscortRotationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_member_id", nullable = false)
    private RotationMember rotationMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EscortRotationAssignment() {
    }

    public EscortRotationAssignment(RotationMember rotationMember, Batch batch, Faculty faculty) {
        this.rotationMember = rotationMember;
        this.batch = batch;
        this.faculty = faculty;
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

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
