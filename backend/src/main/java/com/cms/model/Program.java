package com.cms.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.ProgramDocumentCategory;
import com.cms.model.enums.ProgramStatus;

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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "programs")
@EntityListeners(AuditingEntityListener.class)
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "duration_years", nullable = false)
    private Integer durationYears;

    @Column(name = "seat_capacity")
    private Integer seatCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgramStatus status = ProgramStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_pattern", nullable = false, length = 20)
    private AssessmentPattern assessmentPattern = AssessmentPattern.TERM_BASED;

    @Column(name = "minimum_age_years", nullable = false)
    private Integer minimumAgeYears = 17;

    @Column(name = "age_cutoff_day", nullable = false)
    private Integer ageCutoffDay = 31;

    @Column(name = "age_cutoff_month", nullable = false)
    private Integer ageCutoffMonth = 12;

    @Column(name = "uses_clinical_shift_scheduling", nullable = false)
    private Boolean usesClinicalShiftScheduling = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "program_document_types",
        joinColumns = @JoinColumn(name = "program_id")
    )
    private Set<ProgramDocumentRequirement> documentRequirements = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Program() {
    }

    public Program(String name, String code, Integer durationYears) {
        this.name = name;
        this.code = code;
        this.durationYears = durationYears;
        this.status = ProgramStatus.ACTIVE;
    }

    public Program(String name, String code, Integer durationYears, ProgramStatus status) {
        this.name = name;
        this.code = code;
        this.durationYears = durationYears;
        this.status = status != null ? status : ProgramStatus.ACTIVE;
    }

    public Program(String name, String code, Integer durationYears, ProgramStatus status,
                   AssessmentPattern assessmentPattern) {
        this.name = name;
        this.code = code;
        this.durationYears = durationYears;
        this.status = status != null ? status : ProgramStatus.ACTIVE;
        this.assessmentPattern = assessmentPattern != null ? assessmentPattern : AssessmentPattern.TERM_BASED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getDurationYears() { return durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }
    public Integer getSeatCapacity() { return seatCapacity; }
    public void setSeatCapacity(Integer seatCapacity) { this.seatCapacity = seatCapacity; }
    public ProgramStatus getStatus() { return status; }
    public void setStatus(ProgramStatus status) { this.status = status; }

    public AssessmentPattern getAssessmentPattern() { return assessmentPattern; }
    public void setAssessmentPattern(AssessmentPattern assessmentPattern) {
        this.assessmentPattern = assessmentPattern != null ? assessmentPattern : AssessmentPattern.TERM_BASED;
    }

    public Integer getMinimumAgeYears() { return minimumAgeYears; }
    public void setMinimumAgeYears(Integer minimumAgeYears) { this.minimumAgeYears = minimumAgeYears; }
    public Integer getAgeCutoffDay() { return ageCutoffDay; }
    public void setAgeCutoffDay(Integer ageCutoffDay) { this.ageCutoffDay = ageCutoffDay; }
    public Integer getAgeCutoffMonth() { return ageCutoffMonth; }
    public void setAgeCutoffMonth(Integer ageCutoffMonth) { this.ageCutoffMonth = ageCutoffMonth; }

    public Boolean getUsesClinicalShiftScheduling() { return usesClinicalShiftScheduling; }
    public void setUsesClinicalShiftScheduling(Boolean usesClinicalShiftScheduling) {
        this.usesClinicalShiftScheduling = usesClinicalShiftScheduling != null && usesClinicalShiftScheduling;
    }

    @Transient
    public Integer getTotalTerms() {
        if (durationYears == null) return null;
        AssessmentPattern pattern = assessmentPattern != null ? assessmentPattern : AssessmentPattern.TERM_BASED;
        return pattern == AssessmentPattern.YEARLY ? durationYears : durationYears * 2;
    }

    /** @deprecated Use {@link #getTotalTerms()} instead */
    @Deprecated
    @Transient
    public Integer getTotalSemesters() {
        return getTotalTerms();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Set<ProgramDocumentRequirement> getDocumentRequirements() {
        return documentRequirements;
    }

    /**
     * Replace document requirements in place.
     *
     * Why: Hibernate tracks the @ElementCollection via the *original* PersistentSet.
     * Reassigning to a new HashSet drops the dirty-tracking reference; clear + addAll
     * keeps it intact so the join-table updates are actually flushed.
     */
    public void setDocumentRequirements(Set<ProgramDocumentRequirement> requirements) {
        if (this.documentRequirements == null) {
            this.documentRequirements = new HashSet<>();
        }
        this.documentRequirements.clear();
        if (requirements != null) {
            this.documentRequirements.addAll(requirements);
        }
    }

    public Set<DocumentType> getMandatoryDocumentTypes() {
        return documentRequirements.stream()
            .filter(r -> r.getCategory() == ProgramDocumentCategory.MANDATORY)
            .map(ProgramDocumentRequirement::getDocumentType)
            .collect(Collectors.toSet());
    }

    public Set<DocumentType> getOptionalDocumentTypes() {
        return documentRequirements.stream()
            .filter(r -> r.getCategory() == ProgramDocumentCategory.OPTIONAL)
            .map(ProgramDocumentRequirement::getDocumentType)
            .collect(Collectors.toSet());
    }

    /** All configured document types regardless of category. */
    public Set<DocumentType> getAllConfiguredDocumentTypes() {
        return documentRequirements.stream()
            .map(ProgramDocumentRequirement::getDocumentType)
            .collect(Collectors.toSet());
    }
}
