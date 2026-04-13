package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AttendanceReportResponse;
import com.cms.dto.AttendanceRequest;
import com.cms.dto.AttendanceResponse;
import com.cms.dto.BulkAttendanceRequest;
import com.cms.service.AttendanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<AttendanceResponse> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.markAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<List<AttendanceResponse>> markBulkAttendance(
            @Valid @RequestBody BulkAttendanceRequest request) {
        List<AttendanceResponse> responses = attendanceService.markBulkAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> findAttendance(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) LocalDate date) {
        List<AttendanceResponse> attendances;
        if (studentId != null && courseId != null) {
            attendances = attendanceService.findByStudentIdAndCourseId(studentId, courseId);
        } else if (courseId != null && date != null) {
            attendances = attendanceService.findByCourseIdAndDate(courseId, date);
        } else if (studentId != null) {
            attendances = attendanceService.findByStudentId(studentId);
        } else if (courseId != null) {
            attendances = attendanceService.findByCourseId(courseId);
        } else {
            throw new IllegalArgumentException("At least one filter parameter is required");
        }
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/reports")
    public ResponseEntity<AttendanceReportResponse> getAttendanceReport(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        AttendanceReportResponse report = attendanceService.getAttendanceReport(studentId, courseId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<List<AttendanceReportResponse>> getLowAttendanceAlerts(
            @RequestParam Long courseId) {
        List<AttendanceReportResponse> alerts = attendanceService.getLowAttendanceAlerts(courseId);
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<AttendanceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
