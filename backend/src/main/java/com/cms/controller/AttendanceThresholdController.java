package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AttendanceThresholdDto;
import com.cms.dto.AttendanceThresholdRequest;
import com.cms.service.AttendanceThresholdService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/attendance-thresholds")
public class AttendanceThresholdController {

    private final AttendanceThresholdService service;

    public AttendanceThresholdController(AttendanceThresholdService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@perm.has('ATTENDANCE_THRESHOLD_VIEW')")
    public ResponseEntity<List<AttendanceThresholdDto>> getThresholds(
            @RequestParam Long curriculumTermCourseId) {
        return ResponseEntity.ok(service.getThresholdsForMapping(curriculumTermCourseId));
    }

    @PutMapping
    @PreAuthorize("@perm.has('ATTENDANCE_THRESHOLD_MANAGE')")
    public ResponseEntity<AttendanceThresholdDto> upsertThreshold(
            @Valid @RequestBody AttendanceThresholdRequest request) {
        return ResponseEntity.ok(service.upsertThreshold(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ATTENDANCE_THRESHOLD_MANAGE')")
    public ResponseEntity<Void> deleteThreshold(@PathVariable Long id) {
        service.deleteThreshold(id);
        return ResponseEntity.noContent().build();
    }
}
