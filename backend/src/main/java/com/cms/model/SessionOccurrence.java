package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.OccurrenceStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
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

    @OneToMany(mappedBy = "sessionOccurrence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionOccurrenceUnit> unitCoverages = new ArrayList<>();

    /** Null unless a substitute was applied for this date -- the recurring ClassSchedule.faculty
     *  is never mutated by the absence/substitution feature (Phase 6); this is the one-date-only
     *  override instead. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_faculty_id")
    private Faculty effectiveFaculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_absence_id")
    private FacultyAbsence facultyAbsence;

    @Enumerated(EnumType.STRING)
    @Column(name = "occurrence_status", nullable = false, length = 20)
    private OccurrenceStatus occurrenceStatus = OccurrenceStatus.HELD;

    /** The other session's occurrence row when this one is one half of a Phase 7 staff-to-staff
     *  swap (null otherwise, including for a Phase 6 absence-substitute, which has no partner). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_partner_occurrence_id")
    private SessionOccurrence swapPartnerOccurrence;

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

    public List<SessionOccurrenceUnit> getUnitCoverages() {
        return unitCoverages;
    }

    public void setUnitCoverages(List<SessionOccurrenceUnit> unitCoverages) {
        this.unitCoverages = unitCoverages;
    }

    public Faculty getEffectiveFaculty() {
        return effectiveFaculty;
    }

    public void setEffectiveFaculty(Faculty effectiveFaculty) {
        this.effectiveFaculty = effectiveFaculty;
    }

    public FacultyAbsence getFacultyAbsence() {
        return facultyAbsence;
    }

    public void setFacultyAbsence(FacultyAbsence facultyAbsence) {
        this.facultyAbsence = facultyAbsence;
    }

    public OccurrenceStatus getOccurrenceStatus() {
        return occurrenceStatus;
    }

    public void setOccurrenceStatus(OccurrenceStatus occurrenceStatus) {
        this.occurrenceStatus = occurrenceStatus;
    }

    public SessionOccurrence getSwapPartnerOccurrence() {
        return swapPartnerOccurrence;
    }

    public void setSwapPartnerOccurrence(SessionOccurrence swapPartnerOccurrence) {
        this.swapPartnerOccurrence = swapPartnerOccurrence;
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
