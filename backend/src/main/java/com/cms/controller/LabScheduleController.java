package com.cms.controller;

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

import com.cms.dto.LabScheduleRequest;
import com.cms.dto.LabScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.LabScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/lab-schedules")
public class LabScheduleController {

    private final LabScheduleService labScheduleService;

    public LabScheduleController(LabScheduleService labScheduleService) {
        this.labScheduleService = labScheduleService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<LabScheduleResponse> create(@Valid @RequestBody LabScheduleRequest request) {
        LabScheduleResponse response = labScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LabScheduleResponse>> findAll(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) DayOfWeek dayOfWeek) {
        List<LabScheduleResponse> schedules;
        if (labId != null) {
            schedules = labScheduleService.findByLabId(labId);
        } else if (facultyId != null) {
            schedules = labScheduleService.findByFacultyId(facultyId);
        } else if (batchName != null) {
            schedules = labScheduleService.findByBatchName(batchName);
        } else if (dayOfWeek != null) {
            schedules = labScheduleService.findByDayOfWeek(dayOfWeek);
        } else {
            schedules = labScheduleService.findAll();
        }
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabScheduleResponse> findById(@PathVariable Long id) {
        LabScheduleResponse response = labScheduleService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-conflicts")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<ScheduleConflictResponse> checkConflicts(
            @Valid @RequestBody LabScheduleRequest request) {
        ScheduleConflictResponse response = labScheduleService.checkConflicts(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<LabScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LabScheduleRequest request) {
        LabScheduleResponse response = labScheduleService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
