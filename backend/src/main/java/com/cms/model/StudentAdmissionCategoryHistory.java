package com.cms.model;

import java.time.Instant;

import com.cms.model.enums.AdmissionCategory;

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
@Table(name = "student_admission_category_history")
public class StudentAdmissionCategoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_category", nullable = false, length = 40)
    private AdmissionCategory oldCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_category", nullable = false, length = 40)
    private AdmissionCategory newCategory;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    public StudentAdmissionCategoryHistory() {
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public AdmissionCategory getOldCategory() {
        return oldCategory;
    }

    public void setOldCategory(AdmissionCategory oldCategory) {
        this.oldCategory = oldCategory;
    }

    public AdmissionCategory getNewCategory() {
        return newCategory;
    }

    public void setNewCategory(AdmissionCategory newCategory) {
        this.newCategory = newCategory;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
