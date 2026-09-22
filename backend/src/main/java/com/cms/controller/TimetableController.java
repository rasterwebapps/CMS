package com.cms.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ClinicalShiftSummaryItem;
import com.cms.dto.CohortTermStatusSummary;
import com.cms.dto.ConflictAcknowledgmentStatusResponse;
import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.ResourceGridRowResponse;
import com.cms.dto.SwapCandidateResponse;
import com.cms.dto.SwapRequest;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableApproveRequest;
import com.cms.dto.TimetableCohortActionRequest;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.ClassScheduleService;
import com.cms.service.PersonalTimetableService;
import com.cms.service.ProfileService;
import com.cms.service.ResourceGridService;
import com.cms.service.TimetableConflictInspectorService;
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
    private final TimetableConflictInspectorService timetableConflictInspectorService;

    public TimetableController(TimetableGenerationService timetableGenerationService,
                                TimetableSwapService timetableSwapService,
                                ClassScheduleService classScheduleService,
                                PersonalTimetableService personalTimetableService,
                                ProfileService profileService,
                                TimetableOccurrenceService timetableOccurrenceService,
                                ResourceGridService resourceGridService,
                                TimetableSkeletonService timetableSkeletonService,
                                TimetableConflictInspectorService timetableConflictInspectorService) {
        this.timetableGenerationService = timetableGenerationService;
        this.timetableSwapService = timetableSwapService;
        this.classScheduleService = classScheduleService;
        this.personalTimetableService = personalTimetableService;
        this.profileService = profileService;
        this.timetableOccurrenceService = timetableOccurrenceService;
        this.resourceGridService = resourceGridService;
        this.timetableSkeletonService = timetableSkeletonService;
        this.timetableConflictInspectorService = timetableConflictInspectorService;
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
    @PreAuthorize("@perm.hasAny('TIMETABLE_VIEW', 'MY_TIMETABLE_VIEW', 'MY_TIMETABLE_VIEW_STUDENT', 'MY_TIMETABLE_VIEW_STAFF')")
    public ResponseEntity<MyTimetableResponse> findMyTimetable(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) LocalDate weekStart) {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        return ResponseEntity.ok(personalTimetableService.findMyTimetable(identity, termInstanceId, weekStart));
    }

    // scope=browse (the default) returns every section's published sessions for the whole term and
    // must stay behind the broader TIMETABLE_VIEW; the MY_TIMETABLE_VIEW* permissions only ever
    // unlock scope=personal (self-scoped via PersonalTimetableService), so none of them can be used
    // to widen into scope=browse.
    @GetMapping("/occurrences")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW') or (#scope == 'personal' and @perm.hasAny('MY_TIMETABLE_VIEW', 'MY_TIMETABLE_VIEW_STUDENT', 'MY_TIMETABLE_VIEW_STAFF'))")
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

    // Timetable Builder's "All cohorts" landing summary table (OC-260 folded the former Draft
    // Review screen's own identical table in here) -- one row per cohort enrolled in this term
    // instance with its aggregate DRAFT/PUBLISHED/PARTIALLY_PUBLISHED status plus the cohort's
    // Draft/Generated -> Conflicts Resolved -> Published readiness (see CohortTermStatusSummary).
    // TIMETABLE_VIEW, not TIMETABLE_MANAGE -- this is now Timetable Builder's own landing list and
    // must be visible to the same broader audience as the rest of that screen; the higher-stakes
    // actions each row's status drives (Publish/Revert/Discard) are separately permission-gated on
    // their own endpoints.
    //
    // Paginated (OC-262) the same way every other OneCMS list screen is -- page/size query params,
    // Spring's own Page<> serialized directly (no custom wrapper, matching UnifiedReceiptService's
    // pattern). cohortId optionally narrows to one cohort so the screen's single-cohort filter goes
    // through this same paginated path rather than a separate unpaginated fetch.
    @GetMapping("/draft/cohort-status-summary")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<Page<CohortTermStatusSummary>> findCohortStatusSummary(
            @RequestParam Long termInstanceId,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(timetableGenerationService.getCohortTermStatusSummaryWithReadiness(
            termInstanceId, cohortId, PageRequest.of(page, size)));
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<List<ClassScheduleResponse>> findPublished(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(withClinicalShiftEntries(
            classScheduleService.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED),
            termInstanceId, ClassScheduleStatus.PUBLISHED));
    }

    // TIMETABLE_PUBLISH is its own dedicated permission (OC-260 split it out of the generic
    // TIMETABLE_MANAGE, which otherwise also covers build/edit actions) -- see the operation-wise
    // permission mapping hard gate. A plain approve can still hit an incomplete-coverage gap (see
    // TimetableCoverageGapException) -- overriding it needs its own dedicated permission on top,
    // checked here rather than inside the service so an unauthorized override attempt never
    // reaches business logic at all.
    @PostMapping("/{termInstanceId}/approve")
    @PreAuthorize("@perm.has('TIMETABLE_PUBLISH') and (!#request.overrideIncompleteCoverage() or @perm.has('TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE'))")
    public ResponseEntity<TimetableActionResponse> approve(@PathVariable Long termInstanceId,
                                                            @RequestBody TimetableApproveRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(timetableGenerationService.approve(
            termInstanceId, request.cohortIds(), actor(jwt), request.overrideIncompleteCoverage(), request.overrideReason()));
    }

    // OC-260: became cohort-scoped, so a bodyless DELETE can no longer express "which cohorts" --
    // moved to a body-bearing POST, matching revert-to-draft's own shape below. TIMETABLE_DISCARD_DRAFT
    // is its own dedicated permission, split out of TIMETABLE_MANAGE for the same reason as Publish.
    @PostMapping("/{termInstanceId}/discard-draft")
    @PreAuthorize("@perm.has('TIMETABLE_DISCARD_DRAFT')")
    public ResponseEntity<TimetableActionResponse> clear(@PathVariable Long termInstanceId,
                                                          @RequestBody TimetableCohortActionRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(timetableGenerationService.clear(termInstanceId, request.cohortIds(), actor(jwt)));
    }

    @PostMapping("/{termInstanceId}/revert-to-draft")
    @PreAuthorize("@perm.has('TIMETABLE_DISCARD_PUBLISHED')")
    public ResponseEntity<TimetableActionResponse> revertToDraft(@PathVariable Long termInstanceId,
                                                                  @RequestBody TimetableCohortActionRequest request,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(timetableGenerationService.revertToDraft(termInstanceId, request.cohortIds(), actor(jwt)));
    }

    // OC-260: per-cohort counterpart of Conflict Inspector's own term-wide "Proceed to Review" --
    // reuses the same dedicated TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE permission (V528) rather
    // than inventing a new one, since it's the same operation just scoped to one cohort.
    @GetMapping("/{termInstanceId}/cohorts/{cohortId}/conflict-status")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<ConflictAcknowledgmentStatusResponse> getCohortConflictStatus(
            @PathVariable Long termInstanceId, @PathVariable Long cohortId) {
        return ResponseEntity.ok(timetableConflictInspectorService.getCohortAcknowledgmentStatus(termInstanceId, cohortId));
    }

    @PostMapping("/{termInstanceId}/cohorts/{cohortId}/acknowledge-conflicts")
    @PreAuthorize("@perm.has('TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE')")
    public ResponseEntity<ConflictAcknowledgmentStatusResponse> acknowledgeCohortConflicts(
            @PathVariable Long termInstanceId, @PathVariable Long cohortId) {
        return ResponseEntity.ok(timetableConflictInspectorService.acknowledgeCohort(termInstanceId, cohortId));
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
