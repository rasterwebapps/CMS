package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.BloodGroup;
import com.cms.model.enums.CommunityCategory;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StudentRequest(
    @NotBlank(message = "Roll number is required")
    @Size(max = 50, message = "Roll number must not exceed 50 characters")
    String rollNumber,

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

    @NotNull(message = "Semester is required")
    @Positive(message = "Semester must be positive")
    Integer semester,

    @NotNull(message = "Admission date is required")
    LocalDate admissionDate,

    String labBatch,

    StudentStatus status,

    // Personal information
    LocalDate dateOfBirth,
    Gender gender,
    String aadharNumber,

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
    AddressRequest address
) {}
