package com.cms.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated summary for the main dashboard screen.
 */
public record DashboardSummaryResponse(
    long totalStudents,
    long totalFaculty,
    long totalDepartments,
    long totalSubjects,
    long totalPrograms,
    long totalLabs,
    long totalEquipment,
    long totalExaminations,
    long totalFeePayments,
    long totalMaintenanceRequests,
    long totalAttendanceRecords,
    Map<String, Long> equipmentByStatus,
    Map<String, Long> maintenanceByStatus,
    Map<String, Long> studentsByStatus,
    Map<String, Long> attendanceByStatus,
    Map<String, Long> enquiryFunnel,
    BigDecimal feeCollectedThisMonth,
    BigDecimal feeOutstanding
) {}

