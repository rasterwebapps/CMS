package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FacultyRequest(
    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code must not exceed 50 characters")
    String employeeCode,

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String phone,

    @NotNull(message = "Department ID is required")
    Long departmentId,

    @NotNull(message = "Designation is required")
    Designation designation,

    @Size(max = 255, message = "Specialization must not exceed 255 characters")
    String specialization,

    @Size(max = 1000, message = "Lab expertise must not exceed 1000 characters")
    String labExpertise,

    @NotNull(message = "Joining date is required")
    LocalDate joiningDate,

    FacultyStatus status
) {}
