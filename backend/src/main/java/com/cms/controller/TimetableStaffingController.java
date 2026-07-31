package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.service.TimetableStaffingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/staffing")
public class TimetableStaffingController {

    private final TimetableStaffingService timetableStaffingService;

    public TimetableStaffingController(TimetableStaffingService timetableStaffingService) {
        this.timetableStaffingService = timetableStaffingService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<List<UnstaffedCellResponse>> getUnstaffedCells(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableStaffingService.getUnstaffedCells(termInstanceId));
    }

    @PutMapping("/cells/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_STAFFING_MANAGE')")
    public ResponseEntity<UnstaffedCellResponse> staffCell(
            @PathVariable Long id,
            @Valid @RequestBody StaffingAssignmentRequest request) {
        return ResponseEntity.ok(timetableStaffingService.staffCell(id, request));
    }
}
