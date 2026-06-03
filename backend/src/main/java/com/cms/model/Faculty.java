package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.BankAccountType;
import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyQualification;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.FacultyType;
import com.cms.model.enums.Gender;
import com.cms.model.enums.MaritalStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
@Table(name = "faculty")
@EntityListeners(AuditingEntityListener.class)
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "emergency_contact_name",         length = 100)
    private String emergencyContactName;
    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;
    @Column(name = "emergency_contact_phone",        length = 20)
    private String emergencyContactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speciality_id", nullable = false)
    private Speciality speciality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Designation designation;

    private String specialization;

    @Column(name = "lab_expertise", length = 1000)
    private String labExpertise;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacultyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "faculty_type")
    private FacultyType facultyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "highest_qualification", length = 50)
    private FacultyQualification highestQualification;

    // ── Identity & demographics ────────────────────────────────
    @Column(name = "nrts_number", unique = true)
    private String nrtsNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    private String nationality;
    private String religion;

    @Column(name = "blood_group")
    private String bloodGroup;

    // ── Bank details ───────────────────────────────────────────
    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code")
    private String bankIfscCode;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_account_type")
    private BankAccountType bankAccountType;

    // ── Address (uses shared embeddable, same column names as Student) ─
    @Embedded
    private Address address;

    // ── Experience breakdown (years, NUMERIC(5,1)) ─────────────
    @Column(name = "teaching_exp_ug_years")
    private BigDecimal teachingExperienceUgYears;

    @Column(name = "teaching_exp_pg_years")
    private BigDecimal teachingExperiencePgYears;

    @Column(name = "teaching_exp_phd_years")
    private BigDecimal teachingExperiencePhdYears;

    @Column(name = "clinical_exp_ug_years")
    private BigDecimal clinicalExperienceUgYears;

    @Column(name = "clinical_exp_pg_years")
    private BigDecimal clinicalExperiencePgYears;

    @Column(name = "clinical_exp_phd_years")
    private BigDecimal clinicalExperiencePhdYears;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Faculty() {
    }

    public Faculty(String employeeCode, String firstName, String lastName, String email,
                   String phone, Speciality speciality, Designation designation,
                   String specialization, String labExpertise, LocalDate joiningDate,
                   FacultyStatus status) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.speciality = speciality;
        this.designation = designation;
        this.specialization = specialization;
        this.labExpertise = labExpertise;
        this.joiningDate = joiningDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getEmergencyContactName()         { return emergencyContactName; }
    public void setEmergencyContactName(String v)   { this.emergencyContactName = v; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String v) { this.emergencyContactRelationship = v; }
    public String getEmergencyContactPhone()        { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String v)  { this.emergencyContactPhone = v; }

    public Speciality getSpeciality() { return speciality; }
    public void setSpeciality(Speciality speciality) { this.speciality = speciality; }

    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getLabExpertise() { return labExpertise; }
    public void setLabExpertise(String labExpertise) { this.labExpertise = labExpertise; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public FacultyStatus getStatus() { return status; }
    public void setStatus(FacultyStatus status) { this.status = status; }

    public FacultyType getFacultyType() { return facultyType; }
    public void setFacultyType(FacultyType facultyType) { this.facultyType = facultyType; }

    public FacultyQualification getHighestQualification() { return highestQualification; }
    public void setHighestQualification(FacultyQualification highestQualification) { this.highestQualification = highestQualification; }

    public String getNrtsNumber() { return nrtsNumber; }
    public void setNrtsNumber(String nrtsNumber) { this.nrtsNumber = nrtsNumber; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankIfscCode() { return bankIfscCode; }
    public void setBankIfscCode(String bankIfscCode) { this.bankIfscCode = bankIfscCode; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; }

    public BankAccountType getBankAccountType() { return bankAccountType; }
    public void setBankAccountType(BankAccountType bankAccountType) { this.bankAccountType = bankAccountType; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public BigDecimal getTeachingExperienceUgYears() { return teachingExperienceUgYears; }
    public void setTeachingExperienceUgYears(BigDecimal v) { this.teachingExperienceUgYears = v; }

    public BigDecimal getTeachingExperiencePgYears() { return teachingExperiencePgYears; }
    public void setTeachingExperiencePgYears(BigDecimal v) { this.teachingExperiencePgYears = v; }

    public BigDecimal getTeachingExperiencePhdYears() { return teachingExperiencePhdYears; }
    public void setTeachingExperiencePhdYears(BigDecimal v) { this.teachingExperiencePhdYears = v; }

    public BigDecimal getClinicalExperienceUgYears() { return clinicalExperienceUgYears; }
    public void setClinicalExperienceUgYears(BigDecimal v) { this.clinicalExperienceUgYears = v; }

    public BigDecimal getClinicalExperiencePgYears() { return clinicalExperiencePgYears; }
    public void setClinicalExperiencePgYears(BigDecimal v) { this.clinicalExperiencePgYears = v; }

    public BigDecimal getClinicalExperiencePhdYears() { return clinicalExperiencePhdYears; }
    public void setClinicalExperiencePhdYears(BigDecimal v) { this.clinicalExperiencePhdYears = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
