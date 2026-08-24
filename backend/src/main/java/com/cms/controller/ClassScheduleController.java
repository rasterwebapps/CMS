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

import com.cms.dto.ClassScheduleRequest;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.ClassScheduleService;

import jakarta.validation.Valid;

/** Route/permission codes intentionally kept as `/lab-schedules` and `LAB_SCHEDULE_*` — this
 *  screen now edits both THEORY and LAB sessions (see the sessionType field), but renaming the
 *  route or permission codes would mean re-touching every role's role_permissions row for no
 *  functional gain. See ClassSchedule for the underlying rename rationale. */
@RestController
@RequestMapping("/lab-schedules")
public class ClassScheduleController {

    private final ClassScheduleService classScheduleService;

    public ClassScheduleController(ClassScheduleService classScheduleService) {
        this.classScheduleService = classScheduleService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ClassScheduleResponse> create(@Valid @RequestBody ClassScheduleRequest request) {
        ClassScheduleResponse response = classScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClassScheduleResponse>> findAll(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) DayOfWeek dayOfWeek) {
        List<ClassScheduleResponse> schedules;
        if (facultyId != null && termInstanceId != null) {
            // Faculty Detail's Lab Schedules tab -- scoped to both PUBLISHED and DRAFT, same
            // convention as everywhere else this session (see ClassScheduleService's javadoc).
            schedules = classScheduleService.findByFacultyIdAndTermInstanceId(facultyId, termInstanceId);
        } else if (labId != null) {
            schedules = classScheduleService.findByLabId(labId);
        } else if (facultyId != null) {
            schedules = classScheduleService.findByFacultyId(facultyId);
        } else if (batchName != null) {
            schedules = classScheduleService.findByBatchName(batchName);
        } else if (dayOfWeek != null) {
            schedules = classScheduleService.findByDayOfWeek(dayOfWeek);
        } else {
            schedules = classScheduleService.findAll();
        }
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/by-term/{termInstanceId}")
    public ResponseEntity<List<ClassScheduleResponse>> findByTermInstance(@PathVariable Long termInstanceId) {
        return ResponseEntity.ok(classScheduleService.findByTermInstanceId(termInstanceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassScheduleResponse> findById(@PathVariable Long id) {
        ClassScheduleResponse response = classScheduleService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-conflicts")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ScheduleConflictResponse> checkConflicts(
            @Valid @RequestBody ClassScheduleRequest request) {
        ScheduleConflictResponse response = classScheduleService.checkConflicts(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ClassScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClassScheduleRequest request) {
        ClassScheduleResponse response = classScheduleService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
