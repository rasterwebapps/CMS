package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "admissions")
@EntityListeners(AuditingEntityListener.class)
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "joining_academic_year_id", nullable = false)
    private AcademicYear joiningAcademicYear;

    @Column(name = "enquiry_id", nullable = false)
    private Long enquiryId;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;


    @Column(name = "declaration_place")
    private String declarationPlace;

    @Column(name = "declaration_date")
    private LocalDate declarationDate;

    @Column(name = "parent_consent_given")
    private Boolean parentConsentGiven;

    @Column(name = "applicant_consent_given")
    private Boolean applicantConsentGiven;

    @OneToMany(mappedBy = "admission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcademicQualification> academicQualifications = new ArrayList<>();

    @OneToMany(mappedBy = "admission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdmissionDocument> documents = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Admission() {
    }

    public Admission(Student student, AcademicYear joiningAcademicYear, LocalDate applicationDate) {
        this.student = student;
        this.joiningAcademicYear = joiningAcademicYear;
        this.applicationDate = applicationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public AcademicYear getJoiningAcademicYear() {
        return joiningAcademicYear;
    }

    public void setJoiningAcademicYear(AcademicYear joiningAcademicYear) {
        this.joiningAcademicYear = joiningAcademicYear;
    }

    public Long getEnquiryId() {
        return enquiryId;
    }

    public void setEnquiryId(Long enquiryId) {
        this.enquiryId = enquiryId;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }


    public String getDeclarationPlace() {
        return declarationPlace;
    }

    public void setDeclarationPlace(String declarationPlace) {
        this.declarationPlace = declarationPlace;
    }

    public LocalDate getDeclarationDate() {
        return declarationDate;
    }

    public void setDeclarationDate(LocalDate declarationDate) {
        this.declarationDate = declarationDate;
    }

    public Boolean getParentConsentGiven() {
        return parentConsentGiven;
    }

    public void setParentConsentGiven(Boolean parentConsentGiven) {
        this.parentConsentGiven = parentConsentGiven;
    }

    public Boolean getApplicantConsentGiven() {
        return applicantConsentGiven;
    }

    public void setApplicantConsentGiven(Boolean applicantConsentGiven) {
        this.applicantConsentGiven = applicantConsentGiven;
    }

    public List<AcademicQualification> getAcademicQualifications() {
        return academicQualifications;
    }

    public void setAcademicQualifications(List<AcademicQualification> academicQualifications) {
        this.academicQualifications = academicQualifications;
    }

    public void addAcademicQualification(AcademicQualification qualification) {
        academicQualifications.add(qualification);
        qualification.setAdmission(this);
    }

    public void removeAcademicQualification(AcademicQualification qualification) {
        academicQualifications.remove(qualification);
        qualification.setAdmission(null);
    }

    public List<AdmissionDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<AdmissionDocument> documents) {
        this.documents = documents;
    }

    public void addDocument(AdmissionDocument document) {
        documents.add(document);
        document.setAdmission(this);
    }

    public void removeDocument(AdmissionDocument document) {
        documents.remove(document);
        document.setAdmission(null);
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
