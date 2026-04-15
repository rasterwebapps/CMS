package com.cms.dto;

import java.math.BigDecimal;

public record AttendanceReportResponse(
    Long studentId,
    String studentName,
    String rollNumber,
    Long subjectId,
    String subjectName,
    String subjectCode,
    long totalClasses,
    long classesAttended,
    BigDecimal attendancePercentage,
    boolean lowAttendance
) {}
