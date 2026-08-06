package com.cms.model;

import java.time.Instant;

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
 * A CohortRoomAllocation's Theory room commitment, split into one or more sections when a cohort
 * is too big for any single classroom. Every commit produces at least one section, even the
 * common unsectioned case (exactly one section) -- the allocation header itself no longer carries
 * a classroom directly. Lab/Clinical batches (see {@link Batch#getCohortSection()}) scope
 * themselves to a specific section once a cohort is sectioned, since each section is then its own
 * sub-cohort for batch-splitting purposes.
 */
@Entity
@Table(name = "cohort_sections")
@EntityListeners(AuditingEntityListener.class)
public class CohortSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_room_allocation_id", nullable = false)
    private CohortRoomAllocation cohortRoomAllocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @Column(name = "section_label", nullable = false, length = 100)
    private String sectionLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "planned_size", nullable = false)
    private Integer plannedSize;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CohortSection() {
    }

    public CohortSection(CohortRoomAllocation cohortRoomAllocation, TermInstance termInstance,
                          String sectionLabel, Classroom classroom, Integer plannedSize) {
        this.cohortRoomAllocation = cohortRoomAllocation;
        this.termInstance = termInstance;
        this.sectionLabel = sectionLabel;
        this.classroom = classroom;
        this.plannedSize = plannedSize;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CohortRoomAllocation getCohortRoomAllocation() {
        return cohortRoomAllocation;
    }

    public void setCohortRoomAllocation(CohortRoomAllocation cohortRoomAllocation) {
        this.cohortRoomAllocation = cohortRoomAllocation;
    }

    public TermInstance getTermInstance() {
        return termInstance;
    }

    public void setTermInstance(TermInstance termInstance) {
        this.termInstance = termInstance;
    }

    public String getSectionLabel() {
        return sectionLabel;
    }

    public void setSectionLabel(String sectionLabel) {
        this.sectionLabel = sectionLabel;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public Integer getPlannedSize() {
        return plannedSize;
    }

    public void setPlannedSize(Integer plannedSize) {
        this.plannedSize = plannedSize;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
