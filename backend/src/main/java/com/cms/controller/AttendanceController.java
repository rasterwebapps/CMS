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
import com.cms.dto.AvailableSubjectResponse;
import com.cms.dto.BulkAttendanceRequest;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.StudentRosterResponse;
import com.cms.service.AttendanceService;
import com.cms.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ProfileService profileService;

    public AttendanceController(AttendanceService attendanceService, ProfileService profileService) {
        this.attendanceService = attendanceService;
        this.profileService = profileService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('ATTENDANCE_MANAGE')")
    public ResponseEntity<AttendanceResponse> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.markAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @PreAuthorize("@perm.has('ATTENDANCE_MANAGE')")
    public ResponseEntity<List<AttendanceResponse>> markBulkAttendance(
            @Valid @RequestBody BulkAttendanceRequest request) {
        List<AttendanceResponse> responses = attendanceService.markBulkAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> findAttendance(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) LocalDate date) {
        List<AttendanceResponse> attendances;
        if (studentId != null && subjectId != null) {
            attendances = attendanceService.findByStudentIdAndSubjectId(studentId, subjectId);
        } else if (subjectId != null && date != null) {
            attendances = attendanceService.findBySubjectIdAndDate(subjectId, date);
        } else if (studentId != null) {
            attendances = attendanceService.findByStudentId(studentId);
        } else if (subjectId != null) {
            attendances = attendanceService.findBySubjectId(subjectId);
        } else {
            throw new IllegalArgumentException("At least one filter parameter is required");
        }
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/available-subjects")
    public ResponseEntity<List<AvailableSubjectResponse>> findAvailableSubjects(@RequestParam LocalDate date) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(attendanceService.findAvailableSubjects(identity.entityId(), date));
    }

    @GetMapping("/subject-roster")
    public ResponseEntity<List<StudentRosterResponse>> findRosterForSubject(@RequestParam Long subjectId) {
        return ResponseEntity.ok(attendanceService.findRosterForSubject(subjectId));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<AttendanceReportResponse>> getAttendanceReport(
            @RequestParam Long studentId,
            @RequestParam Long subjectId) {
        List<AttendanceReportResponse> report = attendanceService.getAttendanceReport(studentId, subjectId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/alerts")
    @PreAuthorize("@perm.has('ATTENDANCE_MANAGE')")
    public ResponseEntity<List<AttendanceReportResponse>> getLowAttendanceAlerts(
            @RequestParam Long subjectId) {
        List<AttendanceReportResponse> alerts = attendanceService.getLowAttendanceAlerts(subjectId);
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('ATTENDANCE_MANAGE')")
    public ResponseEntity<AttendanceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ATTENDANCE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
