package com.cms.model;

import java.math.BigDecimal;
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
@Table(name = "attendance_thresholds",
    uniqueConstraints = @UniqueConstraint(columnNames = {"curriculum_term_course_id", "attendance_type"}))
@EntityListeners(AuditingEntityListener.class)
public class AttendanceThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_term_course_id", nullable = false)
    private CurriculumSemesterCourse curriculumSemesterCourse;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false, length = 20)
    private AttendanceType attendanceType;

    @Column(name = "min_percentage", nullable = false)
    private BigDecimal minPercentage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AttendanceThreshold() {
    }

    public AttendanceThreshold(CurriculumSemesterCourse curriculumSemesterCourse,
                               AttendanceType attendanceType, BigDecimal minPercentage) {
        this.curriculumSemesterCourse = curriculumSemesterCourse;
        this.attendanceType = attendanceType;
        this.minPercentage = minPercentage;
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

    public AttendanceType getAttendanceType() {
        return attendanceType;
    }

    public void setAttendanceType(AttendanceType attendanceType) {
        this.attendanceType = attendanceType;
    }

    public BigDecimal getMinPercentage() {
        return minPercentage;
    }

    public void setMinPercentage(BigDecimal minPercentage) {
        this.minPercentage = minPercentage;
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
