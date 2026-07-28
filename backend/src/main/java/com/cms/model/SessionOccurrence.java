package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * "This recurring {@link ClassSchedule} row actually happened on this specific date" anchor — the
 * shared spine for portion-completion progress (this table's original purpose) and, later, Phase
 * 6's faculty-absence substitution (extended additively onto this same table rather than a second
 * one, per the Round 2 plan). Rows are created lazily, only once something is actually logged for
 * a date — never pre-populated for every theoretical occurrence.
 */
@Entity
@Table(name = "session_occurrences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_schedule_id", "occurrence_date"}))
@EntityListeners(AuditingEntityListener.class)
public class SessionOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_faculty_id")
    private Faculty recordedByFaculty;

    @Column(length = 1000)
    private String remarks;

    @ManyToMany
    @JoinTable(name = "session_occurrence_units",
        joinColumns = @JoinColumn(name = "session_occurrence_id"),
        inverseJoinColumns = @JoinColumn(name = "syllabus_unit_id"))
    private Set<SyllabusUnit> coveredUnits = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SessionOccurrence() {
    }

    public SessionOccurrence(ClassSchedule classSchedule, LocalDate occurrenceDate) {
        this.classSchedule = classSchedule;
        this.occurrenceDate = occurrenceDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassSchedule getClassSchedule() {
        return classSchedule;
    }

    public void setClassSchedule(ClassSchedule classSchedule) {
        this.classSchedule = classSchedule;
    }

    public LocalDate getOccurrenceDate() {
        return occurrenceDate;
    }

    public void setOccurrenceDate(LocalDate occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    public Faculty getRecordedByFaculty() {
        return recordedByFaculty;
    }

    public void setRecordedByFaculty(Faculty recordedByFaculty) {
        this.recordedByFaculty = recordedByFaculty;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Set<SyllabusUnit> getCoveredUnits() {
        return coveredUnits;
    }

    public void setCoveredUnits(Set<SyllabusUnit> coveredUnits) {
        this.coveredUnits = coveredUnits;
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
