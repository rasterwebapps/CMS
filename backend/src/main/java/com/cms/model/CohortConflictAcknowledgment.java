package com.cms.model;

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
import jakarta.persistence.UniqueConstraint;

/** Per-cohort counterpart of {@link TermInstance#getConflictAcknowledgedAt()} — OC-260 replaced
 *  the whole-term-only Publish gate with one that lets each cohort in a term progress through
 *  Draft/Generated -&gt; Conflicts Resolved -&gt; Published independently, so "has this cohort's own
 *  skeleton been freshly re-checked and acknowledged clean" needs its own per-cohort row instead of
 *  the single (termInstanceId) pair {@link TermInstance} carries. Plain {@code Long} columns
 *  (not {@code @ManyToOne}) deliberately, matching {@code announcement_audiences.audience_ref_id}'s
 *  precedent for a lightweight tracking row that never needs to navigate the association. */
@Entity
@Table(name = "cohort_conflict_acknowledgments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"term_instance_id", "cohort_id"}))
@EntityListeners(AuditingEntityListener.class)
public class CohortConflictAcknowledgment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_instance_id", nullable = false)
    private Long termInstanceId;

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_cell_count", nullable = false)
    private Integer acknowledgedCellCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Long getTermInstanceId() {
        return termInstanceId;
    }

    public void setTermInstanceId(Long termInstanceId) {
        this.termInstanceId = termInstanceId;
    }

    public Long getCohortId() {
        return cohortId;
    }

    public void setCohortId(Long cohortId) {
        this.cohortId = cohortId;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Integer getAcknowledgedCellCount() {
        return acknowledgedCellCount;
    }

    public void setAcknowledgedCellCount(Integer acknowledgedCellCount) {
        this.acknowledgedCellCount = acknowledgedCellCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
