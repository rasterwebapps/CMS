package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
 * Groups N {@link RotationSlot} cells (same day/period, different subjects, e.g. "English Lab,
 * Wed P3-4" + "Tamil Lab, Wed P3-4") with N {@link RotationMember} physical groups of students
 * that alternate through them week to week. {@link #cycleLength} equals both the slot count and
 * the member count (a square rotation). {@link #anchorOccurrenceDate} is the first real
 * occurrence date used as the week-0 reference for the parity math in RotationResolverService,
 * so rotation counts actual elapsed weeks rather than raw ISO week numbers.
 */
@Entity
@Table(name = "rotation_groups")
@EntityListeners(AuditingEntityListener.class)
public class RotationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(name = "cycle_length", nullable = false)
    private Integer cycleLength;

    @Column(name = "anchor_occurrence_date", nullable = false)
    private LocalDate anchorOccurrenceDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RotationGroup() {
    }

    public RotationGroup(TermInstance termInstance, String label, Integer cycleLength,
                          LocalDate anchorOccurrenceDate, String createdBy) {
        this.termInstance = termInstance;
        this.label = label;
        this.cycleLength = cycleLength;
        this.anchorOccurrenceDate = anchorOccurrenceDate;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TermInstance getTermInstance() {
        return termInstance;
    }

    public void setTermInstance(TermInstance termInstance) {
        this.termInstance = termInstance;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getCycleLength() {
        return cycleLength;
    }

    public void setCycleLength(Integer cycleLength) {
        this.cycleLength = cycleLength;
    }

    public LocalDate getAnchorOccurrenceDate() {
        return anchorOccurrenceDate;
    }

    public void setAnchorOccurrenceDate(LocalDate anchorOccurrenceDate) {
        this.anchorOccurrenceDate = anchorOccurrenceDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
