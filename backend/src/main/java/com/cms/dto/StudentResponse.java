package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentStatus;

public record StudentResponse(
    Long id,
    String rollNumber,
    String admissionNumber,
    String universityRegistrationNumber,
    String umisNumber,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    Long specializationDepartmentId,
    String specializationDepartmentName,
    Integer yearOfStudy,
    LocalDate admissionDate,
    String labBatch,
    StudentStatus status,

    // Personal information
    LocalDate dateOfBirth,
    Gender gender,

    // Demographics
    String nationality,
    String religion,
    String communityCategory,
    String caste,
    String bloodGroup,

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
    String postalAddress,
    String street,
    String city,
    String district,
    String state,
    String pincode,

    String bio,
    String emergencyContactName,
    String emergencyContactRelationship,
    String emergencyContactPhone,

    Instant createdAt,
    Instant updatedAt
) {
    public StudentResponse(Long id, String rollNumber, String firstName, String lastName, String fullName,
                           String email, String phone, Long programId, String programName, Long courseId,
                           String courseName, Long specializationDepartmentId, String specializationDepartmentName,
                           Integer yearOfStudy, LocalDate admissionDate, String labBatch, StudentStatus status,
                           LocalDate dateOfBirth, Gender gender, String nationality, String religion,
                           String communityCategory, String caste, String bloodGroup, String fatherName,
                           String fatherPhone, String fatherEmail, String motherName, String motherPhone,
                           String motherEmail, String parentMobile, String postalAddress, String street,
                           String city, String district, String state, String pincode,
                           Instant createdAt, Instant updatedAt) {
        this(id, rollNumber, null, null, null, firstName, lastName, fullName, email, phone, programId, programName,
            courseId, courseName, specializationDepartmentId, specializationDepartmentName, yearOfStudy,
            admissionDate, labBatch, status, dateOfBirth, gender, nationality, religion, communityCategory,
            caste, bloodGroup, fatherName, fatherPhone, fatherEmail, motherName, motherPhone, motherEmail,
            parentMobile, false, null, null, postalAddress, street, city, district, state, pincode,
            null, null, null, null, createdAt, updatedAt);
    }
}
