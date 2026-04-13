package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AttendanceAnalyticsReportResponse;
import com.cms.dto.LabUtilizationReportResponse;
import com.cms.dto.StudentPerformanceReportResponse;
import com.cms.service.ReportService;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/lab-utilization")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LabUtilizationReportResponse> getLabUtilizationReport() {
        return ResponseEntity.ok(reportService.getLabUtilizationReport());
    }

    @GetMapping("/student-performance/{studentId}")
    public ResponseEntity<StudentPerformanceReportResponse> getStudentPerformanceReport(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(reportService.getStudentPerformanceReport(studentId));
    }

    @GetMapping("/attendance-analytics")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<AttendanceAnalyticsReportResponse> getAttendanceAnalyticsReport() {
        return ResponseEntity.ok(reportService.getAttendanceAnalyticsReport());
    }
}
