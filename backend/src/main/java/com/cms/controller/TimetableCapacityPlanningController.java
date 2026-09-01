package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CapacityPlanResponse;
import com.cms.dto.FacultyWorkloadOverviewReport;
import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.dto.LabClinicalVenueCapacityResult;
import com.cms.dto.TermCapacityOverviewResponse;
import com.cms.dto.VenueRebalanceApplyRequest;
import com.cms.dto.VenueRebalancePreview;
import com.cms.dto.VenueRebalanceResult;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.PlanningBasis;
import com.cms.service.FacultyWorkloadCapacityService;
import com.cms.service.TimetableCapacityPlanningService;
import com.cms.service.TimetableGlobalAutoScheduleService;

@RestController
@RequestMapping("/timetables/capacity-plan")
public class TimetableCapacityPlanningController {

    private final TimetableCapacityPlanningService timetableCapacityPlanningService;
    private final FacultyWorkloadCapacityService facultyWorkloadCapacityService;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public TimetableCapacityPlanningController(TimetableCapacityPlanningService timetableCapacityPlanningService,
                                                 FacultyWorkloadCapacityService facultyWorkloadCapacityService,
                                                 TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.timetableCapacityPlanningService = timetableCapacityPlanningService;
        this.facultyWorkloadCapacityService = facultyWorkloadCapacityService;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<CapacityPlanResponse> getPlan(@RequestParam Long termInstanceId,
                                                          @RequestParam Long cohortId,
                                                          @RequestParam(required = false) PlanningBasis planningBasis) {
        return ResponseEntity.ok(timetableCapacityPlanningService.getPlan(termInstanceId, cohortId, planningBasis));
    }

    /** Same permission as {@link #getPlan} — a second read view within the same already-gated
     *  Capacity Planner screen, not a new distinct operation. */
    @GetMapping("/faculty-workload")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<FacultyWorkloadReportResponse> getFacultyWorkload(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(facultyWorkloadCapacityService.getTermWorkloadReport(termInstanceId));
    }

    /** Same permission as {@link #getPlan} — the term-total "required vs assigned per faculty"
     *  breakdown, distinct from {@link #getFacultyWorkload}'s per-week figures (that report backs
     *  the weekly hard-cap gate; this one backs the same daily-cap/term-total numbers the Global
     *  Auto-Schedule checklist and Faculty Detail's workload tab already show, just for every
     *  active faculty at once instead of only the ones already in trouble). */
    @GetMapping("/faculty-workload-overview")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<FacultyWorkloadOverviewReport> getFacultyWorkloadOverview(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.getFullFacultyWorkloadOverview(termInstanceId));
    }

    /** Same permission as {@link #getPlan} — the bulk Capacity Auto-Plan screen is a read/navigate
     *  view only; committing still goes through {@code CohortRoomAllocationController}'s own
     *  MANAGE-gated endpoint, unchanged. */
    @GetMapping("/term-overview")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<TermCapacityOverviewResponse> getTermOverview(@RequestParam Long termInstanceId,
                                                                          @RequestParam(required = false) PlanningBasis planningBasis) {
        return ResponseEntity.ok(timetableCapacityPlanningService.getTermOverview(termInstanceId, planningBasis));
    }

    /** Same permission as {@link #getPlan} — feeds Capacity Planner's Venue Utilization panel with
     *  the real weekly-demand-vs-window over/tight classification that gates the "Rebalance now"
     *  panel. {@link #getPlan}'s own per-venue utilization figures are a genuinely different
     *  metric (already-placed schedule cells vs. a fixed Mon-Fri slot grid) and must never be used
     *  for this gating — a venue can show high placed-cell occupancy while comfortably under its
     *  real weekly demand window, or the reverse, before Run Automation has placed anything yet. */
    @GetMapping("/venue-capacity")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<LabClinicalVenueCapacityResult> getVenueCapacity(@RequestParam Long termInstanceId,
                                                                             @RequestParam(required = false) PlanningBasis planningBasis) {
        return ResponseEntity.ok(timetableCapacityPlanningService.computeLabClinicalVenueCapacity(termInstanceId,
            planningBasis != null ? planningBasis : PlanningBasis.SANCTIONED));
    }

    /** Read-only preview for "Rebalance now" — same VIEW permission as the rest of this screen's
     *  reports, since nothing is applied until {@link #applyRebalance}. */
    @GetMapping("/rebalance-preview")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<VenueRebalancePreview> previewRebalance(@RequestParam Long termInstanceId,
                                                                     @RequestParam ClassSessionType sessionType,
                                                                     @RequestParam Long venueId,
                                                                     @RequestParam(required = false) PlanningBasis planningBasis) {
        return ResponseEntity.ok(timetableCapacityPlanningService.previewRebalance(termInstanceId, sessionType, venueId,
            planningBasis != null ? planningBasis : PlanningBasis.SANCTIONED));
    }

    /** Mutates already-committed batches — its own dedicated permission, per this project's
     *  operation-wise permission mapping rule (never shared with the VIEW-tier preview above). */
    @PostMapping("/rebalance")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_REBALANCE')")
    public ResponseEntity<VenueRebalanceResult> applyRebalance(@RequestParam Long termInstanceId,
                                                                 @RequestBody VenueRebalanceApplyRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        return ResponseEntity.ok(timetableCapacityPlanningService.applyRebalance(termInstanceId, request.sessionType(),
            request.venueId(), request.batchIds(), actor));
    }
}
