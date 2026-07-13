package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.AttendanceType;

public record AttendanceReportResponse(
    Long studentId,
    String studentName,
    String rollNumber,
    Long subjectId,
    String subjectName,
    String subjectCode,
    AttendanceType type,
    long totalClasses,
    long classesAttended,
    BigDecimal attendancePercentage,
    BigDecimal thresholdPercentage,
    boolean lowAttendance
) {}
