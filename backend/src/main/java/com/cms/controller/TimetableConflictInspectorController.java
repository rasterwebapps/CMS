package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ConflictScanResponse;
import com.cms.service.TimetableConflictInspectorService;

@RestController
@RequestMapping("/timetables/conflict-inspector")
public class TimetableConflictInspectorController {

    private final TimetableConflictInspectorService timetableConflictInspectorService;

    public TimetableConflictInspectorController(TimetableConflictInspectorService timetableConflictInspectorService) {
        this.timetableConflictInspectorService = timetableConflictInspectorService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_CONFLICT_INSPECTOR_VIEW')")
    public ResponseEntity<ConflictScanResponse> scan(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableConflictInspectorService.scanTerm(termInstanceId));
    }
}
