package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.AttendanceType;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "syllabus_units",
    uniqueConstraints = @UniqueConstraint(columnNames = {"curriculum_term_course_id", "unit_number"}))
@EntityListeners(AuditingEntityListener.class)
public class SyllabusUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_term_course_id", nullable = false)
    private CurriculumSemesterCourse curriculumSemesterCourse;

    @Column(name = "unit_number", nullable = false)
    private Integer unitNumber;

    /** Which of the parent's theory/lab/clinical hour buckets this unit's plannedHours counts
     *  against (reuses AttendanceType rather than inventing a parallel THEORY/LAB/CLINICAL enum). */
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 20)
    private AttendanceType componentType = AttendanceType.THEORY;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "planned_hours")
    private Integer plannedHours;

    @Column(length = 1000)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SyllabusUnit() {
    }

    public SyllabusUnit(CurriculumSemesterCourse curriculumSemesterCourse, Integer unitNumber, String title,
                         AttendanceType componentType, Integer plannedHours, String description, Integer sortOrder) {
        this.curriculumSemesterCourse = curriculumSemesterCourse;
        this.unitNumber = unitNumber;
        this.title = title;
        this.componentType = componentType;
        this.plannedHours = plannedHours;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CurriculumSemesterCourse getCurriculumSemesterCourse() {
        return curriculumSemesterCourse;
    }

    public void setCurriculumSemesterCourse(CurriculumSemesterCourse curriculumSemesterCourse) {
        this.curriculumSemesterCourse = curriculumSemesterCourse;
    }

    public Integer getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(Integer unitNumber) {
        this.unitNumber = unitNumber;
    }

    public AttendanceType getComponentType() {
        return componentType;
    }

    public void setComponentType(AttendanceType componentType) {
        this.componentType = componentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(Integer plannedHours) {
        this.plannedHours = plannedHours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
