package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ApplyRescheduleRequest;
import com.cms.dto.RescheduleResponse;
import com.cms.dto.VenueCandidate;
import com.cms.service.SessionRescheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/reschedule")
public class SessionRescheduleController {

    private final SessionRescheduleService sessionRescheduleService;

    public SessionRescheduleController(SessionRescheduleService sessionRescheduleService) {
        this.sessionRescheduleService = sessionRescheduleService;
    }

    @GetMapping("/sessions/{classScheduleId}/candidates")
    @PreAuthorize("@perm.has('TIMETABLE_OCCURRENCE_RESCHEDULE')")
    public ResponseEntity<List<VenueCandidate>> findCandidates(
            @PathVariable Long classScheduleId,
            @RequestParam LocalDate date,
            @RequestParam LocalDate targetDate,
            @RequestParam Long periodId) {
        return ResponseEntity.ok(
            sessionRescheduleService.findCandidateVenues(classScheduleId, date, targetDate, periodId));
    }

    @PostMapping("/sessions/{classScheduleId}/apply")
    @PreAuthorize("@perm.has('TIMETABLE_OCCURRENCE_RESCHEDULE')")
    public ResponseEntity<RescheduleResponse> apply(
            @PathVariable Long classScheduleId, @Valid @RequestBody ApplyRescheduleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(sessionRescheduleService.apply(classScheduleId, request, actor(jwt)));
    }

    @DeleteMapping("/sessions/{classScheduleId}/reschedule")
    @PreAuthorize("@perm.has('TIMETABLE_OCCURRENCE_RESCHEDULE')")
    public ResponseEntity<RescheduleResponse> revert(
            @PathVariable Long classScheduleId, @RequestParam LocalDate date, @RequestParam LocalDate targetDate,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(sessionRescheduleService.revert(classScheduleId, date, targetDate, actor(jwt)));
    }

    private static String actor(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
