package com.cms.model;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
@Entity
@Table(
    name = "roll_number_sequences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "academic_year"})
)
@EntityListeners(AuditingEntityListener.class)
public class RollNumberSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;
    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    public RollNumberSequence() {
    }
    public RollNumberSequence(Long courseId, Integer academicYear, Integer lastSequence) {
        this.courseId = courseId;
        this.academicYear = academicYear;
        this.lastSequence = lastSequence;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getCourseId() {
        return courseId;
    }
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
    public Integer getAcademicYear() {
        return academicYear;
    }
    public void setAcademicYear(Integer academicYear) {
        this.academicYear = academicYear;
    }
    public Integer getLastSequence() {
        return lastSequence;
    }
    public void setLastSequence(Integer lastSequence) {
        this.lastSequence = lastSequence;
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
