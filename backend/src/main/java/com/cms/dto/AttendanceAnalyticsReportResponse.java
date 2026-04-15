package com.cms.dto;

import java.util.Map;

public record AttendanceAnalyticsReportResponse(
    Long totalStudents,
    Long totalAttendanceRecords,
    Map<String, Long> attendanceByStatus,
    Map<String, Long> attendanceByType
) {
    /** Backward-compatible constructor used in tests. */
    public AttendanceAnalyticsReportResponse(Long totalStudents, Long totalAttendanceRecords) {
        this(totalStudents, totalAttendanceRecords, Map.of(), Map.of());
    }
}
