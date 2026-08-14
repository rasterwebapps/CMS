package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

import com.cms.model.enums.ElectiveSelectionMode;

@Entity
@Table(name = "curriculum_elective_groups",
    uniqueConstraints = @UniqueConstraint(columnNames = {"curriculum_version_id", "term_number", "group_code"}))
@EntityListeners(AuditingEntityListener.class)
public class CurriculumElectiveGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_version_id", nullable = false)
    private CurriculumVersion curriculumVersion;

    @Column(name = "term_number", nullable = false)
    private Integer termNumber;

    @Column(name = "group_name", nullable = false, length = 150)
    private String groupName;

    @Column(name = "group_code", length = 50)
    private String groupCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false, length = 30)
    private ElectiveSelectionMode selectionMode = ElectiveSelectionMode.STUDENT_CHOICE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CurriculumElectiveGroup() {
    }

    public CurriculumElectiveGroup(CurriculumVersion curriculumVersion, Integer termNumber,
                                    String groupName, String groupCode) {
        this.curriculumVersion = curriculumVersion;
        this.termNumber = termNumber;
        this.groupName = groupName;
        this.groupCode = groupCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CurriculumVersion getCurriculumVersion() {
        return curriculumVersion;
    }

    public void setCurriculumVersion(CurriculumVersion curriculumVersion) {
        this.curriculumVersion = curriculumVersion;
    }

    public Integer getTermNumber() {
        return termNumber;
    }

    public void setTermNumber(Integer termNumber) {
        this.termNumber = termNumber;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public ElectiveSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(ElectiveSelectionMode selectionMode) {
        this.selectionMode = selectionMode;
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
