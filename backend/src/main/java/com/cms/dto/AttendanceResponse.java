package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;

public record AttendanceResponse(
    Long id,
    Long studentId,
    String studentName,
    String rollNumber,
    Long subjectId,
    String subjectName,
    String subjectCode,
    LocalDate date,
    AttendanceStatus status,
    AttendanceType type,
    String remarks,
    Long markedById,
    String markedByName,
    Instant createdAt,
    Instant updatedAt
) {}
