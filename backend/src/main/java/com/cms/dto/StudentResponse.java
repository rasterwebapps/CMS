package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.BloodGroup;
import com.cms.model.enums.CommunityCategory;
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
    CommunityCategory communityCategory,
    String caste,
    BloodGroup bloodGroup,

    // Family information
    String fatherName,
    String motherName,
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
