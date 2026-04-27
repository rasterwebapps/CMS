package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.ResultStatus;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "semester_results",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_term_enrollment_id"}))
@EntityListeners(AuditingEntityListener.class)
public class SemesterResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_term_enrollment_id", nullable = false)
    private StudentTermEnrollment studentTermEnrollment;

    @Column(name = "total_max_marks", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalMaxMarks;

    @Column(name = "total_marks_obtained", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalMarksObtained;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private ResultStatus resultStatus = ResultStatus.NOT_PUBLISHED;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SemesterResult() {}

    public SemesterResult(StudentTermEnrollment studentTermEnrollment, BigDecimal totalMaxMarks,
                          BigDecimal totalMarksObtained, BigDecimal percentage, ResultStatus resultStatus) {
        this.studentTermEnrollment = studentTermEnrollment;
        this.totalMaxMarks = totalMaxMarks;
        this.totalMarksObtained = totalMarksObtained;
        this.percentage = percentage;
        this.resultStatus = resultStatus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentTermEnrollment getStudentTermEnrollment() { return studentTermEnrollment; }
    public void setStudentTermEnrollment(StudentTermEnrollment studentTermEnrollment) { this.studentTermEnrollment = studentTermEnrollment; }
    public BigDecimal getTotalMaxMarks() { return totalMaxMarks; }
    public void setTotalMaxMarks(BigDecimal totalMaxMarks) { this.totalMaxMarks = totalMaxMarks; }
    public BigDecimal getTotalMarksObtained() { return totalMarksObtained; }
    public void setTotalMarksObtained(BigDecimal totalMarksObtained) { this.totalMarksObtained = totalMarksObtained; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public ResultStatus getResultStatus() { return resultStatus; }
    public void setResultStatus(ResultStatus resultStatus) { this.resultStatus = resultStatus; }
    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
