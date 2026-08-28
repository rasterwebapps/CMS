package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.model.enums.WeekOfMonth;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "term_instances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year_id", "term_type"}))
@EntityListeners(AuditingEntityListener.class)
public class TermInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false, length = 10)
    private TermType termType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TermInstanceStatus status = TermInstanceStatus.PLANNED;

    /** Which nth-Saturday-of-the-month occurrences count as real working days for this term.
     *  Empty means no restriction is configured -- Saturday is not used by automation at all
     *  (Mon-Fri only) until an admin opts in by picking at least one value here; once non-empty,
     *  ONLY Saturdays matching one of these ever get placed or produce a real class occurrence —
     *  see TimetableBlockedPeriodChecker/ClassScheduleOccurrenceService's isSaturdayWorkingDay. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "term_working_saturdays", joinColumns = @JoinColumn(name = "term_instance_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "week_of_month")
    private Set<WeekOfMonth> workingSaturdayWeeks = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TermInstance() {
    }

    public TermInstance(AcademicYear academicYear, TermType termType,
                        LocalDate startDate, LocalDate endDate, TermInstanceStatus status) {
        this.academicYear = academicYear;
        this.termType = termType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public TermType getTermType() {
        return termType;
    }

    public void setTermType(TermType termType) {
        this.termType = termType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public TermInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(TermInstanceStatus status) {
        this.status = status;
    }

    public Set<WeekOfMonth> getWorkingSaturdayWeeks() {
        return workingSaturdayWeeks;
    }

    public void setWorkingSaturdayWeeks(Set<WeekOfMonth> workingSaturdayWeeks) {
        this.workingSaturdayWeeks = workingSaturdayWeeks;
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
