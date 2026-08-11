package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ApplyStaffSwapRequest;
import com.cms.dto.StaffSwapCandidateResponse;
import com.cms.service.FacultySessionSwapService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/staff-swap")
public class FacultySessionSwapController {

    private final FacultySessionSwapService facultySessionSwapService;

    public FacultySessionSwapController(FacultySessionSwapService facultySessionSwapService) {
        this.facultySessionSwapService = facultySessionSwapService;
    }

    @GetMapping("/sessions/{classScheduleId}/candidates")
    @PreAuthorize("@perm.has('TIMETABLE_STAFF_SWAP')")
    public ResponseEntity<List<StaffSwapCandidateResponse>> findCandidates(
            @PathVariable Long classScheduleId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(facultySessionSwapService.findSwapCandidates(classScheduleId, date));
    }

    @PostMapping("/sessions/{classScheduleId}/apply")
    @PreAuthorize("@perm.has('TIMETABLE_STAFF_SWAP')")
    public ResponseEntity<Void> applySwap(
            @PathVariable Long classScheduleId, @Valid @RequestBody ApplyStaffSwapRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        facultySessionSwapService.applySwap(classScheduleId, request.targetClassScheduleId(), request.date(),
            jwt != null ? jwt.getClaimAsString("preferred_username") : "system");
        return ResponseEntity.noContent().build();
    }
}
