package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.Designation;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.FacultyQualification;

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

@Entity
@Table(name = "faculty_document_type_requirements")
@EntityListeners(AuditingEntityListener.class)
public class FacultyDocumentTypeRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 80)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private FacultyQualification qualification;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FacultyDocumentTypeRequirement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }

    public Speciality getSpeciality() { return speciality; }
    public void setSpeciality(Speciality speciality) { this.speciality = speciality; }

    public FacultyQualification getQualification() { return qualification; }
    public void setQualification(FacultyQualification qualification) { this.qualification = qualification; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
