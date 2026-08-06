package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.PlanningBasis;

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
 * A Cohort's committed physical-location claim for a Term, term-scoped (no day/period here — that
 * belongs to the later Staffing pass). The actual Theory room(s) live on child
 * {@link CohortSection} rows (fetched via {@code CohortSectionRepository}, not a JPA relation
 * here, mirroring how {@link Batch#getCohortRoomAllocation()} is fetched) — every commit produces
 * at least one section, even the common unsectioned case. Lab/Clinical venue assignments are not
 * on this header either, for the same reason — see {@link Batch#getCohortRoomAllocation()}.
 * {@link #planningBasis}/{@link #plannedStrength} record which strength number (live enrolled
 * headcount vs. university-sanctioned intake) this commit was actually planned against, since a
 * cohort's live enrollment can keep changing after the fact. Reverting sets {@link #status} to
 * REVERTED and soft-deactivates every Batch/CohortSection this allocation created rather than
 * deleting, so roster history survives.
 */
@Entity
@Table(name = "cohort_room_allocations")
@EntityListeners(AuditingEntityListener.class)
public class CohortRoomAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_instance_id", nullable = false)
    private TermInstance termInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CohortRoomAllocationStatus status = CohortRoomAllocationStatus.COMMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "planning_basis", nullable = false, length = 20)
    private PlanningBasis planningBasis = PlanningBasis.ENROLLED;

    @Column(name = "planning_strength", nullable = false)
    private Integer plannedStrength = 0;

    @Column(name = "committed_by")
    private String committedBy;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt;

    @Column(name = "reverted_by")
    private String revertedBy;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CohortRoomAllocation() {
    }

    public CohortRoomAllocation(Cohort cohort, TermInstance termInstance, PlanningBasis planningBasis,
                                 Integer plannedStrength, String committedBy) {
        this.cohort = cohort;
        this.termInstance = termInstance;
        this.planningBasis = planningBasis;
        this.plannedStrength = plannedStrength;
        this.committedBy = committedBy;
        this.committedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    public TermInstance getTermInstance() {
        return termInstance;
    }

    public void setTermInstance(TermInstance termInstance) {
        this.termInstance = termInstance;
    }

    public PlanningBasis getPlanningBasis() {
        return planningBasis;
    }

    public void setPlanningBasis(PlanningBasis planningBasis) {
        this.planningBasis = planningBasis;
    }

    public Integer getPlannedStrength() {
        return plannedStrength;
    }

    public void setPlannedStrength(Integer plannedStrength) {
        this.plannedStrength = plannedStrength;
    }

    public CohortRoomAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(CohortRoomAllocationStatus status) {
        this.status = status;
    }

    public String getCommittedBy() {
        return committedBy;
    }

    public void setCommittedBy(String committedBy) {
        this.committedBy = committedBy;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public String getRevertedBy() {
        return revertedBy;
    }

    public void setRevertedBy(String revertedBy) {
        this.revertedBy = revertedBy;
    }

    public Instant getRevertedAt() {
        return revertedAt;
    }

    public void setRevertedAt(Instant revertedAt) {
        this.revertedAt = revertedAt;
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
