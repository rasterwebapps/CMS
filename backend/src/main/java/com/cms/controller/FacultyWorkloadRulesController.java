package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FacultyWorkloadRulesRequest;
import com.cms.dto.FacultyWorkloadRulesResponse;
import com.cms.service.FacultyWorkloadRulesService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/workload-rules")
public class FacultyWorkloadRulesController {

    private final FacultyWorkloadRulesService facultyWorkloadRulesService;

    public FacultyWorkloadRulesController(FacultyWorkloadRulesService facultyWorkloadRulesService) {
        this.facultyWorkloadRulesService = facultyWorkloadRulesService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_WORKLOAD_RULES_VIEW') or @perm.has('TIMETABLE_WORKLOAD_RULES_MANAGE')")
    public ResponseEntity<FacultyWorkloadRulesResponse> get() {
        return ResponseEntity.ok(facultyWorkloadRulesService.get());
    }

    @PutMapping
    @PreAuthorize("@perm.has('TIMETABLE_WORKLOAD_RULES_MANAGE')")
    public ResponseEntity<FacultyWorkloadRulesResponse> update(@Valid @RequestBody FacultyWorkloadRulesRequest request) {
        return ResponseEntity.ok(facultyWorkloadRulesService.update(request));
    }
}
