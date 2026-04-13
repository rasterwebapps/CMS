package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lab_continuous_evaluations")
@EntityListeners(AuditingEntityListener.class)
public class LabContinuousEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "record_marks")
    private Integer recordMarks;

    @Column(name = "viva_marks")
    private Integer vivaMarks;

    @Column(name = "performance_marks")
    private Integer performanceMarks;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "evaluation_date")
    private LocalDate evaluationDate;

    @Column(name = "evaluated_by")
    private String evaluatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LabContinuousEvaluation() {}

    public LabContinuousEvaluation(Experiment experiment, Student student, Integer recordMarks,
                                    Integer vivaMarks, Integer performanceMarks, Integer totalMarks,
                                    LocalDate evaluationDate, String evaluatedBy) {
        this.experiment = experiment;
        this.student = student;
        this.recordMarks = recordMarks;
        this.vivaMarks = vivaMarks;
        this.performanceMarks = performanceMarks;
        this.totalMarks = totalMarks;
        this.evaluationDate = evaluationDate;
        this.evaluatedBy = evaluatedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Experiment getExperiment() { return experiment; }
    public void setExperiment(Experiment experiment) { this.experiment = experiment; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Integer getRecordMarks() { return recordMarks; }
    public void setRecordMarks(Integer recordMarks) { this.recordMarks = recordMarks; }
    public Integer getVivaMarks() { return vivaMarks; }
    public void setVivaMarks(Integer vivaMarks) { this.vivaMarks = vivaMarks; }
    public Integer getPerformanceMarks() { return performanceMarks; }
    public void setPerformanceMarks(Integer performanceMarks) { this.performanceMarks = performanceMarks; }
    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }
    public LocalDate getEvaluationDate() { return evaluationDate; }
    public void setEvaluationDate(LocalDate evaluationDate) { this.evaluationDate = evaluationDate; }
    public String getEvaluatedBy() { return evaluatedBy; }
    public void setEvaluatedBy(String evaluatedBy) { this.evaluatedBy = evaluatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
