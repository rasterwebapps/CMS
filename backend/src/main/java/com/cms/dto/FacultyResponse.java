package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyStatus;

public record FacultyResponse(
    Long id,
    String employeeCode,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phone,
    Long departmentId,
    String departmentName,
    Designation designation,
    String specialization,
    String labExpertise,
    LocalDate joiningDate,
    FacultyStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
