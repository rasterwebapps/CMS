package com.cms.controller;

import java.time.LocalDate;
import java.util.ArrayList;
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

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ClinicalShiftSummaryItem;
import com.cms.dto.CohortTermStatusSummary;
import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.ResourceGridRowResponse;
import com.cms.dto.SwapCandidateResponse;
import com.cms.dto.SwapRequest;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableApproveRequest;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.ClassScheduleService;
import com.cms.service.PersonalTimetableService;
import com.cms.service.ProfileService;
import com.cms.service.ResourceGridService;
import com.cms.service.TimetableGenerationService;
import com.cms.service.TimetableOccurrenceService;
import com.cms.service.TimetableSkeletonService;
import com.cms.service.TimetableSwapService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableGenerationService timetableGenerationService;
    private final TimetableSwapService timetableSwapService;
    private final ClassScheduleService classScheduleService;
    private final PersonalTimetableService personalTimetableService;
    private final ProfileService profileService;
    private final TimetableOccurrenceService timetableOccurrenceService;
    private final ResourceGridService resourceGridService;
    private final TimetableSkeletonService timetableSkeletonService;

    public TimetableController(TimetableGenerationService timetableGenerationService,
                                TimetableSwapService timetableSwapService,
                                ClassScheduleService classScheduleService,
                                PersonalTimetableService personalTimetableService,
                                ProfileService profileService,
                                TimetableOccurrenceService timetableOccurrenceService,
                                ResourceGridService resourceGridService,
                                TimetableSkeletonService timetableSkeletonService) {
        this.timetableGenerationService = timetableGenerationService;
        this.timetableSwapService = timetableSwapService;
        this.classScheduleService = classScheduleService;
        this.personalTimetableService = personalTimetableService;
        this.profileService = profileService;
        this.timetableOccurrenceService = timetableOccurrenceService;
        this.resourceGridService = resourceGridService;
        this.timetableSkeletonService = timetableSkeletonService;
    }

    @GetMapping("/resource-grid/faculty")
    @PreAuthorize("@perm.has('TIMETABLE_FACULTY_GRID_VIEW')")
    public ResponseEntity<List<ResourceGridRowResponse>> getFacultyResourceGrid(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(resourceGridService.getResourceGrid(
            ResourceGridService.ResourceType.FACULTY, termInstanceId, dayOfWeek, date));
    }

    @GetMapping("/resource-grid/classroom")
    @PreAuthorize("@perm.has('TIMETABLE_CLASSROOM_GRID_VIEW')")
    public ResponseEntity<List<ResourceGridRowResponse>> getClassroomResourceGrid(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(resourceGridService.getResourceGrid(
            ResourceGridService.ResourceType.CLASSROOM, termInstanceId, dayOfWeek, date));
    }

    @GetMapping("/me")
    @PreAuthorize("@perm.hasAny('TIMETABLE_VIEW', 'MY_TIMETABLE_VIEW')")
    public ResponseEntity<MyTimetableResponse> findMyTimetable(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) LocalDate weekStart) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(personalTimetableService.findMyTimetable(identity, termInstanceId, weekStart));
    }

    // scope=browse (the default) returns every section's published sessions for the whole term and
    // must stay behind the broader TIMETABLE_VIEW; MY_TIMETABLE_VIEW only ever unlocks scope=personal
    // (self-scoped via PersonalTimetableService), so it can never be used to widen into scope=browse.
    @GetMapping("/occurrences")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW') or (#scope == 'personal' and @perm.has('MY_TIMETABLE_VIEW'))")
    public ResponseEntity<List<ClassScheduleOccurrenceResponse>> findOccurrences(
            @RequestParam Long termInstanceId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "browse") String scope) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(
            timetableOccurrenceService.findOccurrences(identity, termInstanceId, from, to, scope));
    }

    @GetMapping("/draft")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<List<ClassScheduleResponse>> findDraft(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(withClinicalShiftEntries(
            classScheduleService.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT),
            termInstanceId, ClassScheduleStatus.DRAFT));
    }

    // Clinical Shift Group duty rosters never produce a real ClassSchedule row, so without this
    // the review/browse grid looked incomplete -- a cohort's whole Clinical component was only
    // hinted at via the separate duty-roster banner instead of shown alongside Theory/Lab, leaving
    // an admin unable to see the complete generated timetable before approving/publishing it.
    private List<ClassScheduleResponse> withClinicalShiftEntries(List<ClassScheduleResponse> rows,
                                                                   Long termInstanceId, ClassScheduleStatus status) {
        List<ClassScheduleResponse> merged = new ArrayList<>(rows);
        merged.addAll(timetableSkeletonService.findClinicalShiftGridEntries(termInstanceId, status));
        return merged;
    }

    // Clinical Shift Group hours never produce a ClassSchedule row (see ClinicalShiftSummaryItem),
    // so /draft above can never surface them -- this feeds Draft Review's duty-roster banner instead.
    @GetMapping("/draft/clinical-shift-summary")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<List<ClinicalShiftSummaryItem>> findClinicalShiftSummary(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableSkeletonService.findClinicalShiftSummaryForTerm(termInstanceId));
    }

    // Draft Review's landing summary table -- one row per cohort enrolled in this term instance
    // with its aggregate DRAFT/PUBLISHED/PARTIALLY_PUBLISHED status (see CohortTermStatusSummary).
    @GetMapping("/draft/cohort-status-summary")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<List<CohortTermStatusSummary>> findCohortStatusSummary(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableSkeletonService.getCohortTermStatusSummary(termInstanceId));
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<List<ClassScheduleResponse>> findPublished(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(withClinicalShiftEntries(
            classScheduleService.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED),
            termInstanceId, ClassScheduleStatus.PUBLISHED));
    }

    // A plain "TIMETABLE_MANAGE" approve can still hit an incomplete-coverage gap (see
    // TimetableCoverageGapException) -- overriding it needs its own dedicated permission per the
    // operation-wise permission mapping hard gate, checked here rather than inside the service so
    // an unauthorized override attempt never reaches business logic at all.
    @PostMapping("/{termInstanceId}/approve")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE') and (#request == null or !#request.overrideIncompleteCoverage() or @perm.has('TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE'))")
    public ResponseEntity<TimetableActionResponse> approve(@PathVariable Long termInstanceId,
                                                            @RequestBody(required = false) TimetableApproveRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        boolean override = request != null && request.overrideIncompleteCoverage();
        String overrideReason = request != null ? request.overrideReason() : null;
        return ResponseEntity.ok(timetableGenerationService.approve(termInstanceId, actor(jwt), override, overrideReason));
    }

    @DeleteMapping("/{termInstanceId}")
    @PreAuthorize("@perm.has('TIMETABLE_MANAGE')")
    public ResponseEntity<TimetableActionResponse> clear(@PathVariable Long termInstanceId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(timetableGenerationService.clear(termInstanceId, actor(jwt)));
    }

    @PostMapping("/{termInstanceId}/revert-to-draft")
    @PreAuthorize("@perm.has('TIMETABLE_DISCARD_PUBLISHED')")
    public ResponseEntity<TimetableActionResponse> revertToDraft(@PathVariable Long termInstanceId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(timetableGenerationService.revertToDraft(termInstanceId, actor(jwt)));
    }

    @GetMapping("/{termInstanceId}/sessions/{sessionId}/swap-candidates")
    @PreAuthorize("@perm.has('TIMETABLE_SWAP')")
    public ResponseEntity<List<SwapCandidateResponse>> findSwapCandidates(
            @PathVariable Long termInstanceId, @PathVariable Long sessionId) {
        return ResponseEntity.ok(timetableSwapService.findCandidates(termInstanceId, sessionId));
    }

    @PostMapping("/{termInstanceId}/sessions/{sessionId}/swap")
    @PreAuthorize("@perm.has('TIMETABLE_SWAP')")
    public ResponseEntity<Void> swap(
            @PathVariable Long termInstanceId, @PathVariable Long sessionId, @Valid @RequestBody SwapRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        timetableSwapService.swap(termInstanceId, sessionId, request, actor(jwt));
        return ResponseEntity.noContent().build();
    }

    /** Null in some WebMvcTest slices that disable the security filter chain (see
     *  ImportController for the same established pattern) -- never null in real traffic, where
     *  every one of these endpoints is already gated by {@code @PreAuthorize}. */
    private static String actor(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
