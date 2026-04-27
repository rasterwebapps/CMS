package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.MarkStatus;

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
@Table(name = "student_marks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_event_id", "course_registration_id"}))
@EntityListeners(AuditingEntityListener.class)
public class StudentMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_event_id", nullable = false)
    private ExamEvent examEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_registration_id", nullable = false)
    private CourseRegistration courseRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_status", nullable = false, length = 20)
    private MarkStatus markStatus = MarkStatus.ABSENT;

    @Column(name = "marks_obtained", precision = 10, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StudentMark() {}

    public StudentMark(ExamEvent examEvent, CourseRegistration courseRegistration,
                       MarkStatus markStatus, BigDecimal marksObtained, String remarks) {
        this.examEvent = examEvent;
        this.courseRegistration = courseRegistration;
        this.markStatus = markStatus;
        this.marksObtained = marksObtained;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamEvent getExamEvent() { return examEvent; }
    public void setExamEvent(ExamEvent examEvent) { this.examEvent = examEvent; }
    public CourseRegistration getCourseRegistration() { return courseRegistration; }
    public void setCourseRegistration(CourseRegistration courseRegistration) { this.courseRegistration = courseRegistration; }
    public MarkStatus getMarkStatus() { return markStatus; }
    public void setMarkStatus(MarkStatus markStatus) { this.markStatus = markStatus; }
    public BigDecimal getMarksObtained() { return marksObtained; }
    public void setMarksObtained(BigDecimal marksObtained) { this.marksObtained = marksObtained; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
