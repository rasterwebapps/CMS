package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentStatus;

public record StudentResponse(
    Long id,
    String rollNumber,
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
    Integer semester,
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

    // Address
    String postalAddress,
    String street,
    String city,
    String district,
    String state,
    String pincode,

    Instant createdAt,
    Instant updatedAt
) {}
