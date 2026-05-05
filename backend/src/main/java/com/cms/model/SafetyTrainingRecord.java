package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.cms.model.enums.TraineeType;
import com.cms.model.enums.TrainingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "safety_training_records")
public class SafetyTrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String trainee;

    @Enumerated(EnumType.STRING)
    @Column(name = "trainee_type", nullable = false)
    private TraineeType traineeType;

    @Column(name = "training_title", nullable = false, length = 255)
    private String trainingTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id")
    private Lab lab;

    @Column(name = "conducted_by", nullable = false, length = 255)
    private String conductedBy;

    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingStatus status;

    private Integer score;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SafetyTrainingRecord() {
    }

    public SafetyTrainingRecord(String trainee, TraineeType traineeType, String trainingTitle,
            String conductedBy, LocalDate trainingDate, TrainingStatus status) {
        this.trainee = trainee;
        this.traineeType = traineeType;
        this.trainingTitle = trainingTitle;
        this.conductedBy = conductedBy;
        this.trainingDate = trainingDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrainee() { return trainee; }
    public void setTrainee(String trainee) { this.trainee = trainee; }

    public TraineeType getTraineeType() { return traineeType; }
    public void setTraineeType(TraineeType traineeType) { this.traineeType = traineeType; }

    public String getTrainingTitle() { return trainingTitle; }
    public void setTrainingTitle(String trainingTitle) { this.trainingTitle = trainingTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Lab getLab() { return lab; }
    public void setLab(Lab lab) { this.lab = lab; }

    public String getConductedBy() { return conductedBy; }
    public void setConductedBy(String conductedBy) { this.conductedBy = conductedBy; }

    public LocalDate getTrainingDate() { return trainingDate; }
    public void setTrainingDate(LocalDate trainingDate) { this.trainingDate = trainingDate; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public TrainingStatus getStatus() { return status; }
    public void setStatus(TrainingStatus status) { this.status = status; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

