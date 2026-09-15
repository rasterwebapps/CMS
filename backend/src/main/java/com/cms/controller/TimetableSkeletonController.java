package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CourseOfferingDto;
import com.cms.dto.DutyDayMovePreviewResponse;
import com.cms.dto.DutyDayMoveRequest;
import com.cms.dto.SkeletonRelocateRequest;
import com.cms.dto.SkeletonRelocationPlanResponse;
import com.cms.dto.ElectiveGroupPlacementRequest;
import com.cms.dto.ElectiveGroupScheduleResponse;
import com.cms.dto.GlobalAutoScheduleResult;
import com.cms.dto.GlobalAutoSchedulePrerequisites;
import com.cms.dto.GlobalCapacityPrecheckResult;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellMoveRequest;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellReplaceResponse;
import com.cms.dto.SkeletonCellReplaceRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonCellSwapRequest;
import com.cms.dto.SkeletonPlacementCandidateResponse;
import com.cms.dto.SkeletonSlotPreviewResponse;
import com.cms.model.enums.ClassSessionType;
import com.cms.service.TimetableGlobalAutoScheduleService;
import com.cms.service.TimetableSkeletonService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/skeleton")
public class TimetableSkeletonController {

    private final TimetableSkeletonService timetableSkeletonService;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public TimetableSkeletonController(TimetableSkeletonService timetableSkeletonService,
                                        TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.timetableSkeletonService = timetableSkeletonService;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<SkeletonBuilderResponse> getSkeleton(@RequestParam Long termInstanceId, @RequestParam Long cohortId) {
        return ResponseEntity.ok(timetableSkeletonService.getCohortSkeleton(termInstanceId, cohortId));
    }

    @GetMapping("/suggest")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MANAGE')")
    public ResponseEntity<List<SkeletonPlacementCandidateResponse>> suggestCandidates(
            @RequestParam Long courseOfferingId,
            @RequestParam ClassSessionType sessionType,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long cohortSectionId) {
        return ResponseEntity.ok(timetableSkeletonService.suggestCandidates(courseOfferingId, sessionType, batchId, cohortSectionId));
    }

    /** Manual placement pins the new cell (see {@code placeCellManually}), so the next Global
     *  Auto-Schedule rebuild packs the week around it rather than clearing it. */
    @PostMapping("/cells")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MANAGE')")
    public ResponseEntity<SkeletonCellResponse> placeCell(@Valid @RequestBody SkeletonCellPlacementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableSkeletonService.placeCellManually(request));
    }

    /** Pin a draft cell so automation works around it, or unpin it to hand it back to automation.
     *  Its own permission rather than a free rider on MOVE: unpinning re-exposes the cell to being
     *  overwritten by the next run, a materially different consequence from repositioning it. */
    @PutMapping("/cells/{id}/pin")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_PIN')")
    public ResponseEntity<SkeletonCellResponse> setPinned(@PathVariable Long id, @RequestParam boolean pinned) {
        return ResponseEntity.ok(timetableSkeletonService.setPinned(id, pinned));
    }

    /** Replace what a placed Theory cell teaches, keeping its slot and audience. Its own permission
     *  rather than a free rider on MANAGE: replacing displaces the previous subject, which silently
     *  puts that subject BELOW its curriculum-hours requirement elsewhere in the term — a different
     *  and less obvious consequence than placing or removing a session outright. */
    @PutMapping("/cells/{id}/replace")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_REPLACE')")
    public ResponseEntity<SkeletonCellReplaceResponse> replaceCell(
            @PathVariable Long id, @Valid @RequestBody SkeletonCellReplaceRequest request) {
        return ResponseEntity.ok(timetableSkeletonService.replaceCellSubject(id, request));
    }

    @DeleteMapping("/cells/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MANAGE')")
    public ResponseEntity<Void> removeCell(@PathVariable Long id) {
        timetableSkeletonService.removeCell(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cells/{id}/move")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MOVE')")
    public ResponseEntity<SkeletonCellResponse> moveCell(@PathVariable Long id, @Valid @RequestBody SkeletonCellMoveRequest request) {
        return ResponseEntity.ok(timetableSkeletonService.moveCell(id, request));
    }

    /** Live drag-highlight data: legality of every grid slot for moving this cell, without
     *  actually moving it. Shares {@code TIMETABLE_SKELETON_MOVE} with {@link #moveCell} since
     *  it's a read-only preview of that exact same action, not a distinct capability. */
    @GetMapping("/cells/{id}/move-preview")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MOVE')")
    public ResponseEntity<List<SkeletonSlotPreviewResponse>> previewMoveTargets(
            @PathVariable Long id, @RequestParam Long cohortId) {
        return ResponseEntity.ok(timetableSkeletonService.previewMoveTargets(id, cohortId));
    }

    /** Same gesture as {@link #moveCell} (drag a cell to a new slot) — this is what fires instead
     *  when the target slot is already occupied, so it shares {@code TIMETABLE_SKELETON_MOVE}
     *  rather than being a separate button/permission. */
    @PutMapping("/cells/{id}/swap")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MOVE')")
    public ResponseEntity<List<SkeletonCellResponse>> swapCells(@PathVariable Long id, @Valid @RequestBody SkeletonCellSwapRequest request) {
        return ResponseEntity.ok(timetableSkeletonService.swapCells(id, request));
    }

    /** Drag-highlight and Swap-menu data for moving a session with its whole block: every
     *  same-length window, legal or not (with the reason), and what would move where. A read-only
     *  preview of {@link #relocate}, so it shares {@code TIMETABLE_SKELETON_MOVE}. */
    @GetMapping("/cells/{id}/relocate-preview")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MOVE')")
    public ResponseEntity<List<SkeletonRelocationPlanResponse>> previewRelocation(@PathVariable Long id, @RequestParam Long cohortId) {
        return ResponseEntity.ok(timetableSkeletonService.previewRelocation(id, cohortId));
    }

    /** Move (into empty periods) or swap (with the sessions already there) a session together with
     *  its whole block — the same drag gesture as {@link #moveCell}/{@link #swapCells}, so it shares
     *  {@code TIMETABLE_SKELETON_MOVE}. */
    @PutMapping("/cells/{id}/relocate")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MOVE')")
    public ResponseEntity<List<SkeletonCellResponse>> relocate(@PathVariable Long id, @Valid @RequestBody SkeletonRelocateRequest request) {
        return ResponseEntity.ok(timetableSkeletonService.relocate(id, request));
    }

    /** Which other days a Clinical Shift group's duty could move to, and what would swap. */
    @GetMapping("/clinical-shift-groups/{id}/day-preview")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_DUTY_DAY_MOVE')")
    public ResponseEntity<List<DutyDayMovePreviewResponse>> previewDutyDayMove(@PathVariable Long id, @RequestParam Long cohortId) {
        return ResponseEntity.ok(timetableSkeletonService.previewDutyDayMove(id, cohortId));
    }

    /** Move a Clinical Shift group's duty to another day; that day's sessions inside the duty
     *  window swap into the day it leaves. Its own permission: it changes which day students are
     *  off campus on hospital duty all term. */
    @PutMapping("/clinical-shift-groups/{id}/day")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_DUTY_DAY_MOVE')")
    public ResponseEntity<List<SkeletonCellResponse>> moveDutyDay(@PathVariable Long id, @Valid @RequestBody DutyDayMoveRequest request) {
        return ResponseEntity.ok(timetableSkeletonService.moveDutyDay(id, request));
    }

    /** Read-only, consolidated "is this ready to automate" report — offerings/elective members with
     *  no faculty bound, plus every faculty over capacity. Call this first and surface every
     *  shortfall as an actionable link before offering the Run action, rather than discovering gaps
     *  one gate at a time. {@code cohortId} omitted checks every cohort in the term. */
    @GetMapping("/global-auto-place/prerequisites")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE')")
    public ResponseEntity<GlobalAutoSchedulePrerequisites> checkGlobalAutoPlacePrerequisites(
            @RequestParam Long termInstanceId, @RequestParam(required = false) Long cohortId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.checkPrerequisites(termInstanceId, cohortId));
    }

    /** Read-only — sums every faculty's real total term-hour demand across every offering they're
     *  bound to, across every cohort in the term, against their real term capacity. The frontend
     *  must call this first and never call {@link #globalAutoPlace} if it comes back non-empty. */
    @GetMapping("/global-auto-place/precheck")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE')")
    public ResponseEntity<GlobalCapacityPrecheckResult> precheckGlobalAutoPlace(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.precheckCapacity(termInstanceId));
    }

    /** Places and staffs every cohort's remaining shortfall — best-effort: commits everything it
     *  can and reports the rest via {@link GlobalAutoScheduleResult}'s per-cohort {@code unplaced}
     *  lists rather than aborting the whole run over one unplaceable session. Re-runs the capacity
     *  precheck defensively even though the frontend already called it. {@code cohortId} omitted
     *  runs every cohort enrolled in the term; provided scopes the run to just that cohort. */
    @PostMapping("/global-auto-place")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE')")
    public ResponseEntity<GlobalAutoScheduleResult> globalAutoPlace(
            @RequestParam Long termInstanceId, @RequestParam(required = false) Long cohortId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.runGlobalAutoSchedule(termInstanceId, cohortId));
    }

    /** The run report's "Apply duty length" (OC-227): sets one Course Offering's clinical shift
     *  duration to the minimum that lets its existing duty roster deliver every curriculum Clinical
     *  hour. The minutes are recomputed server-side, never taken from the request. */
    @PostMapping("/clinical-duty-fit/{courseOfferingId}")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_CLINICAL_DUTY_FIT')")
    public ResponseEntity<CourseOfferingDto> applyClinicalDutyFit(@PathVariable Long courseOfferingId) {
        return ResponseEntity.ok(timetableGlobalAutoScheduleService.applyClinicalDutyFit(courseOfferingId));
    }

    @PostMapping("/elective-groups/place")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_ELECTIVE_PLACE')")
    public ResponseEntity<List<SkeletonCellResponse>> placeElectiveGroup(@Valid @RequestBody ElectiveGroupPlacementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableSkeletonService.placeElectiveGroup(request));
    }

    @GetMapping("/elective-groups/{electiveGroupId}/schedule")
    @PreAuthorize("@perm.has('TIMETABLE_VIEW') or @perm.has('COURSE_REGISTRATION_ELECTIVE_ASSIGN')")
    public ResponseEntity<ElectiveGroupScheduleResponse> getElectiveGroupSchedule(
            @PathVariable Long electiveGroupId, @RequestParam Long termInstanceId) {
        return ResponseEntity.ok(timetableSkeletonService.getElectiveGroupSchedule(electiveGroupId, termInstanceId));
    }
}
