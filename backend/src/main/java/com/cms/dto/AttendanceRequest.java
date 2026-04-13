package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;

import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Course ID is required")
    Long courseId,

    @NotNull(message = "Date is required")
    LocalDate date,

    @NotNull(message = "Status is required")
    AttendanceStatus status,

    @NotNull(message = "Type is required")
    AttendanceType type,

    String remarks
) {}
