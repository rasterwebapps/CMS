package com.cms.dto;

import java.math.BigDecimal;

public record AttendanceReportResponse(
    Long studentId,
    String studentName,
    String rollNumber,
    Long courseId,
    String courseName,
    String courseCode,
    long totalClasses,
    long classesAttended,
    BigDecimal attendancePercentage,
    boolean lowAttendance
) {}
