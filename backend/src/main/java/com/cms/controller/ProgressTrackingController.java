package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LogProgressRequest;
import com.cms.dto.OfferingProgressResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.SessionOccurrenceDto;
import com.cms.dto.SyllabusUnitDto;
import com.cms.dto.TermProgressSummaryResponse;
import com.cms.service.ProfileService;
import com.cms.service.ProgressTrackingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/progress-tracking")
public class ProgressTrackingController {

    private final ProgressTrackingService progressTrackingService;
    private final ProfileService profileService;

    public ProgressTrackingController(ProgressTrackingService progressTrackingService,
                                       ProfileService profileService) {
        this.progressTrackingService = progressTrackingService;
        this.profileService = profileService;
    }

    @PostMapping("/log")
    @PreAuthorize("@perm.has('PROGRESS_LOG_CREATE')")
    public ResponseEntity<SessionOccurrenceDto> logCoverage(@Valid @RequestBody LogProgressRequest request) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        Long facultyId = "FACULTY".equals(identity.entityType()) ? identity.entityId() : null;
        return ResponseEntity.ok(progressTrackingService.logCoverage(request, facultyId));
    }

    @GetMapping("/sessions/{classScheduleId}/units")
    @PreAuthorize("@perm.has('PROGRESS_LOG_CREATE')")
    public ResponseEntity<List<SyllabusUnitDto>> getAvailableUnits(@PathVariable Long classScheduleId) {
        return ResponseEntity.ok(progressTrackingService.getAvailableUnits(classScheduleId));
    }

    @GetMapping("/sessions/{classScheduleId}/occurrence-dates")
    @PreAuthorize("@perm.has('PROGRESS_LOG_CREATE')")
    public ResponseEntity<List<LocalDate>> getLoggableOccurrenceDates(
            @PathVariable Long classScheduleId, @RequestParam LocalDate from) {
        return ResponseEntity.ok(progressTrackingService.getLoggableOccurrenceDates(classScheduleId, from));
    }

    @GetMapping("/sessions/{classScheduleId}/occurrences/{date}")
    @PreAuthorize("@perm.has('PROGRESS_LOG_CREATE')")
    public ResponseEntity<SessionOccurrenceDto> getOccurrence(
            @PathVariable Long classScheduleId, @PathVariable LocalDate date) {
        return progressTrackingService.getOccurrence(classScheduleId, date)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/course-offerings/{courseOfferingId}")
    @PreAuthorize("@perm.has('PROGRESS_LOG_CREATE') or @perm.has('PROGRESS_REPORT_VIEW')")
    public ResponseEntity<OfferingProgressResponse> getProgressForOffering(@PathVariable Long courseOfferingId) {
        return ResponseEntity.ok(progressTrackingService.getProgressForOffering(courseOfferingId));
    }

    @GetMapping("/term-instances/{termInstanceId}/summary")
    @PreAuthorize("@perm.has('PROGRESS_REPORT_VIEW')")
    public ResponseEntity<TermProgressSummaryResponse> getOverallProgressSummary(@PathVariable Long termInstanceId) {
        return ResponseEntity.ok(progressTrackingService.getOverallProgressSummary(termInstanceId));
    }
}
