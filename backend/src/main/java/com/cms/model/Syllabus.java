package com.cms.model;

import java.time.Instant;

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
@Table(name = "syllabi")
@EntityListeners(AuditingEntityListener.class)
public class Syllabus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "theory_hours")
    private Integer theoryHours;

    @Column(name = "lab_hours")
    private Integer labHours;

    @Column(name = "tutorial_hours")
    private Integer tutorialHours;

    @Column(length = 2000)
    private String objectives;

    @Column(length = 4000)
    private String content;

    @Column(name = "text_books", length = 2000)
    private String textBooks;

    @Column(name = "reference_books", length = 2000)
    private String referenceBooks;

    @Column(name = "course_outcomes", length = 2000)
    private String courseOutcomes;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Syllabus() {
    }

    public Syllabus(Course course, Integer version, Integer theoryHours, Integer labHours,
                    Integer tutorialHours, String objectives, String content,
                    String textBooks, String referenceBooks, String courseOutcomes, Boolean isActive) {
        this.course = course;
        this.version = version;
        this.theoryHours = theoryHours;
        this.labHours = labHours;
        this.tutorialHours = tutorialHours;
        this.objectives = objectives;
        this.content = content;
        this.textBooks = textBooks;
        this.referenceBooks = referenceBooks;
        this.courseOutcomes = courseOutcomes;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getTheoryHours() {
        return theoryHours;
    }

    public void setTheoryHours(Integer theoryHours) {
        this.theoryHours = theoryHours;
    }

    public Integer getLabHours() {
        return labHours;
    }

    public void setLabHours(Integer labHours) {
        this.labHours = labHours;
    }

    public Integer getTutorialHours() {
        return tutorialHours;
    }

    public void setTutorialHours(Integer tutorialHours) {
        this.tutorialHours = tutorialHours;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTextBooks() {
        return textBooks;
    }

    public void setTextBooks(String textBooks) {
        this.textBooks = textBooks;
    }

    public String getReferenceBooks() {
        return referenceBooks;
    }

    public void setReferenceBooks(String referenceBooks) {
        this.referenceBooks = referenceBooks;
    }

    public String getCourseOutcomes() {
        return courseOutcomes;
    }

    public void setCourseOutcomes(String courseOutcomes) {
        this.courseOutcomes = courseOutcomes;
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
