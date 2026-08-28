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

import java.util.Set;

import com.cms.dto.TermAdvanceChecklistResponse;
import com.cms.dto.TermInstanceDto;
import com.cms.dto.TermInstanceUpdateRequest;
import com.cms.dto.WorkingSaturdaysRequest;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.WeekOfMonth;
import com.cms.service.TermInstanceService;

@RestController
@RequestMapping("/term-instances")
public class TermInstanceController {

    private final TermInstanceService termInstanceService;

    public TermInstanceController(TermInstanceService termInstanceService) {
        this.termInstanceService = termInstanceService;
    }

    @GetMapping
    public ResponseEntity<List<TermInstanceDto>> getByAcademicYear(
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(termInstanceService.getTermInstancesByAcademicYear(academicYearId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TermInstanceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(termInstanceService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SEMESTER_MANAGE')")
    public ResponseEntity<TermInstanceDto> update(
            @PathVariable Long id,
            @RequestBody TermInstanceUpdateRequest request) {
        return ResponseEntity.ok(termInstanceService.updateTermInstance(id, request));
    }

    /** Gated the same as the PUT above (not left open like the two GETs above it) -- this
     *  surfaces outstanding fee amounts, and is only ever useful to someone about to perform the
     *  gated action it previews. */
    @GetMapping("/{id}/advance-checklist")
    @PreAuthorize("@perm.has('SEMESTER_MANAGE')")
    public ResponseEntity<TermAdvanceChecklistResponse> getAdvanceChecklist(
            @PathVariable Long id, @RequestParam TermInstanceStatus targetStatus) {
        return ResponseEntity.ok(termInstanceService.getAdvanceChecklist(id, targetStatus));
    }

    /** Empty means this term hasn't opted in to Saturday scheduling at all — Mon-Fri only, hard
     *  blocked otherwise (see TimetableBlockedPeriodChecker). Read-gated the same as the write
     *  below rather than left open: this is scheduling-policy detail, not general term info. */
    @GetMapping("/{id}/working-saturdays")
    @PreAuthorize("@perm.has('TIMETABLE_WORKING_SATURDAYS_MANAGE')")
    public ResponseEntity<Set<WeekOfMonth>> getWorkingSaturdays(@PathVariable Long id) {
        return ResponseEntity.ok(termInstanceService.getWorkingSaturdays(id));
    }

    @PutMapping("/{id}/working-saturdays")
    @PreAuthorize("@perm.has('TIMETABLE_WORKING_SATURDAYS_MANAGE')")
    public ResponseEntity<Set<WeekOfMonth>> updateWorkingSaturdays(
            @PathVariable Long id, @RequestBody WorkingSaturdaysRequest request) {
        return ResponseEntity.ok(termInstanceService.updateWorkingSaturdays(id, request.weeks()));
    }
}
