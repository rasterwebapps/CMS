package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ConflictAcknowledgmentStatusResponse;
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

    @GetMapping("/acknowledgment-status")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<ConflictAcknowledgmentStatusResponse> acknowledgmentStatus(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableConflictInspectorService.getAcknowledgmentStatus(termInstanceId));
    }

    @PostMapping("/acknowledge")
    @PreAuthorize("@perm.has('TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE')")
    public ResponseEntity<ConflictAcknowledgmentStatusResponse> acknowledge(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableConflictInspectorService.acknowledge(termInstanceId));
    }
}
