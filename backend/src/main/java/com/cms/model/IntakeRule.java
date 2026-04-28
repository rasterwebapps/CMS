package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.TermType;

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

@Entity
@Table(name = "intake_rules")
@EntityListeners(AuditingEntityListener.class)
public class IntakeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "admission_window_start_date", nullable = false)
    private LocalDate admissionWindowStartDate;

    @Column(name = "admission_window_end_date", nullable = false)
    private LocalDate admissionWindowEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapped_academic_year_id", nullable = false)
    private AcademicYear mappedAcademicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapped_start_term_type", nullable = false, length = 10)
    private TermType mappedStartTermType;

    @Column(name = "starting_semester_number", nullable = false)
    private Integer startingSemesterNumber = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IntakeRule() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public LocalDate getAdmissionWindowStartDate() {
        return admissionWindowStartDate;
    }

    public void setAdmissionWindowStartDate(LocalDate admissionWindowStartDate) {
        this.admissionWindowStartDate = admissionWindowStartDate;
    }

    public LocalDate getAdmissionWindowEndDate() {
        return admissionWindowEndDate;
    }

    public void setAdmissionWindowEndDate(LocalDate admissionWindowEndDate) {
        this.admissionWindowEndDate = admissionWindowEndDate;
    }

    public AcademicYear getMappedAcademicYear() {
        return mappedAcademicYear;
    }

    public void setMappedAcademicYear(AcademicYear mappedAcademicYear) {
        this.mappedAcademicYear = mappedAcademicYear;
    }

    public TermType getMappedStartTermType() {
        return mappedStartTermType;
    }

    public void setMappedStartTermType(TermType mappedStartTermType) {
        this.mappedStartTermType = mappedStartTermType;
    }

    public Integer getStartingSemesterNumber() {
        return startingSemesterNumber;
    }

    public void setStartingSemesterNumber(Integer startingSemesterNumber) {
        this.startingSemesterNumber = startingSemesterNumber;
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
