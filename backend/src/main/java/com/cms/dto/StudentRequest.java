package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.AdmissionCategory;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StudentRequest(
    @Size(max = 50, message = "Roll number must not exceed 50 characters")
    String rollNumber,

    @Size(max = 50, message = "University registration number must not exceed 50 characters")
    String universityRegistrationNumber,

    @Size(max = 50, message = "UMIS number must not exceed 50 characters")
    String umisNumber,

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String phone,

    @NotNull(message = "Program ID is required")
    Long programId,

    Long courseId,

    Long specializationDepartmentId,

    @NotNull(message = "Year of study is required")
    @Positive(message = "Year of study must be positive")
    Integer yearOfStudy,

    @NotNull(message = "Admission date is required")
    LocalDate admissionDate,

    String labBatch,

    StudentStatus status,
    AdmissionCategory admissionCategory,

    // Personal information
    LocalDate dateOfBirth,
    Gender gender,
    String aadharNumber,

    // Demographics
    String nationality,
    String religion,
    String communityCategory,
    String caste,
    String bloodGroup,
    Boolean physicalDisability,

    // Family information
    String fatherName,
    String fatherPhone,
    String fatherEmail,
    String motherName,
    String motherPhone,
    String motherEmail,
    String parentMobile,

    // Scholarship eligibility
    Boolean isFirstGraduate,
    String fatherEducation,
    String motherEducation,

    // Address
    AddressRequest address
) {
    public StudentRequest(String rollNumber, String firstName, String lastName, String email,
                          String phone, Long programId, Long courseId, Long specializationDepartmentId,
                          Integer yearOfStudy, LocalDate admissionDate, String labBatch, StudentStatus status,
                          LocalDate dateOfBirth, Gender gender, String aadharNumber, String nationality,
                          String religion, String communityCategory, String caste, String bloodGroup,
                          String fatherName, String fatherPhone, String fatherEmail, String motherName,
                          String motherPhone, String motherEmail, String parentMobile, AddressRequest address) {
        this(rollNumber, null, null, firstName, lastName, email, phone, programId, courseId, specializationDepartmentId,
            yearOfStudy, admissionDate, labBatch, status, null, dateOfBirth, gender, aadharNumber, nationality,
            religion, communityCategory, caste, bloodGroup, false, fatherName, fatherPhone, fatherEmail,
            motherName, motherPhone, motherEmail, parentMobile, false, null, null, address);
    }
}
