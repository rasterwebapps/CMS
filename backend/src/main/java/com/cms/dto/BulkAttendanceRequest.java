package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkAttendanceRequest(
    @NotNull(message = "Subject ID is required")
    Long subjectId,

    @NotNull(message = "Date is required")
    LocalDate date,

    @NotNull(message = "Type is required")
    AttendanceType type,

    @NotEmpty(message = "Student attendance list is required")
    List<StudentAttendance> studentAttendances
) {
    public record StudentAttendance(
        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Status is required")
        AttendanceStatus status,

        String remarks
    ) {}
}
