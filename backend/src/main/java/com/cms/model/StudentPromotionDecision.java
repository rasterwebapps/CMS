package com.cms.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.PromotionOutcome;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

@Entity
@Table(name = "student_promotion_decisions")
@EntityListeners(AuditingEntityListener.class)
public class StudentPromotionDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_term_instance_id", nullable = false)
    private TermInstance fromTermInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_term_instance_id")
    private TermInstance toTermInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionOutcome outcome;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "student_promotion_decision_arrears",
        joinColumns = @JoinColumn(name = "decision_id")
    )
    @Column(name = "subject_id")
    private Set<Long> arrearSubjectIds = new HashSet<>();

    @Column(name = "decided_by", nullable = false, length = 100)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(length = 500)
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StudentPromotionDecision() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Cohort getCohort() { return cohort; }
    public void setCohort(Cohort cohort) { this.cohort = cohort; }
    public TermInstance getFromTermInstance() { return fromTermInstance; }
    public void setFromTermInstance(TermInstance fromTermInstance) { this.fromTermInstance = fromTermInstance; }
    public TermInstance getToTermInstance() { return toTermInstance; }
    public void setToTermInstance(TermInstance toTermInstance) { this.toTermInstance = toTermInstance; }
    public PromotionOutcome getOutcome() { return outcome; }
    public void setOutcome(PromotionOutcome outcome) { this.outcome = outcome; }
    public Set<Long> getArrearSubjectIds() { return arrearSubjectIds; }
    public void setArrearSubjectIds(Set<Long> arrearSubjectIds) { this.arrearSubjectIds = arrearSubjectIds; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
