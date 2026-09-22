package com.cms.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CohortTermStatusSummary;
import com.cms.dto.ConflictScanResponse;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingFacultySummaryDto;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableCoverageGap;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.exception.TimetableCoverageGapException;
import com.cms.model.ClassSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.OfferingAssignmentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.LabAttendanceRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Draft lifecycle actions (clear/approve/revert) for a term's {@link ClassSchedule} rows. R3.1
 * retired the one-shot auto-{@code generate()} this service used to offer — Skeleton Builder
 * (placement) → Staffing (faculty/room) is now the only path that creates DRAFT rows; this
 * service only ever acts on rows that already exist.
 *
 * <p>OC-260 (Timetable Draft Review retired into Skeleton Builder): every action here now takes an
 * explicit, non-empty {@code cohortIds} — publishing/reverting/discarding cohort A must never touch
 * cohort B's rows. There is deliberately no "empty means whole term" shortcut; the caller always
 * resolves and sends the exact cohort selection. {@link TimetableSkeletonService
 * #getCohortActiveClassSchedules} is the single source of truth for which {@link ClassSchedule}
 * rows belong to a cohort — the same resolution Skeleton Builder's own grid and the Conflict
 * Inspector's cohort-scoped scan ({@link TimetableConflictInspectorService#scanCohorts}) use, so
 * this service can never act on a different row set than what the screen showed the admin.
 */
@Service
@Transactional(readOnly = true)
public class TimetableGenerationService {

    private final ClassScheduleRepository classScheduleRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final LabAttendanceRepository labAttendanceRepository;
    private final AuditLogService auditLogService;
    private final TimetableConflictInspectorService timetableConflictInspectorService;
    private final CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;
    private final TimetableStaffingAutoAssignService timetableStaffingAutoAssignService;
    private final TimetableCoverageService timetableCoverageService;
    private final TimetableSkeletonService timetableSkeletonService;
    private final CourseOfferingService courseOfferingService;

    public TimetableGenerationService(ClassScheduleRepository classScheduleRepository,
                                       TermInstanceRepository termInstanceRepository,
                                       LabAttendanceRepository labAttendanceRepository,
                                       AuditLogService auditLogService,
                                       TimetableConflictInspectorService timetableConflictInspectorService,
                                       CourseOfferingSectionFacultyService courseOfferingSectionFacultyService,
                                       TimetableStaffingAutoAssignService timetableStaffingAutoAssignService,
                                       TimetableCoverageService timetableCoverageService,
                                       TimetableSkeletonService timetableSkeletonService,
                                       CourseOfferingService courseOfferingService) {
        this.classScheduleRepository = classScheduleRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.labAttendanceRepository = labAttendanceRepository;
        this.auditLogService = auditLogService;
        this.timetableConflictInspectorService = timetableConflictInspectorService;
        this.courseOfferingSectionFacultyService = courseOfferingSectionFacultyService;
        this.timetableStaffingAutoAssignService = timetableStaffingAutoAssignService;
        this.timetableCoverageService = timetableCoverageService;
        this.timetableSkeletonService = timetableSkeletonService;
        this.courseOfferingService = courseOfferingService;
    }

    private void requireCohortIds(List<Long> cohortIds) {
        if (cohortIds == null || cohortIds.isEmpty()) {
            throw new IllegalArgumentException("At least one cohort must be selected.");
        }
    }

    private Set<Long> resolveCohortScheduleIds(Long termInstanceId, List<Long> cohortIds) {
        Set<Long> ids = new HashSet<>();
        for (Long cohortId : cohortIds) {
            timetableSkeletonService.getCohortActiveClassSchedules(termInstanceId, cohortId)
                .forEach(cs -> ids.add(cs.getId()));
        }
        return ids;
    }

    private Set<Long> resolveCohortOfferingIds(Long termInstanceId, List<Long> cohortIds) {
        Set<Long> ids = new HashSet<>();
        for (Long cohortId : cohortIds) {
            courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId)
                .forEach(o -> ids.add(o.id()));
        }
        return ids;
    }

    /** A LOCKED term's timetable is immutable — clear/approve/revert all refuse once the term
     *  itself has moved past OPEN, independently of whatever {@link ClassScheduleStatus} its rows
     *  are in. */
    private void requireNotLocked(TermInstance term) {
        if (term.getStatus() == TermInstanceStatus.LOCKED) {
            throw new LifecycleConflictException(
                "This term is locked. Its timetable can no longer be changed.",
                "TIMETABLE_TERM_LOCKED", "TermInstance", term.getId(), null);
        }
    }

    private TermInstance requireTermInstance(Long termInstanceId) {
        return termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
    }

    @Transactional
    public TimetableActionResponse clear(Long termInstanceId, List<Long> cohortIds, String actor) {
        TermInstance term = requireTermInstance(termInstanceId);
        requireNotLocked(term);
        requireCohortIds(cohortIds);
        Set<Long> scheduleIds = resolveCohortScheduleIds(termInstanceId, cohortIds);
        // Mirrors revertToDraft's own attendance guard below -- a hard delete is strictly more
        // dangerous than a revert-to-draft for the exact same reason: lab_attendances.lab_schedule_id
        // has no ON DELETE/status-transition handling, so wiping attendance-backed sessions would
        // orphan that history's session linkage. Scoped to the selected cohorts' own rows -- OC-260
        // made this action cohort-scoped, so attendance recorded against a different, unselected
        // cohort's sessions must never block discarding this one's draft.
        if (!scheduleIds.isEmpty() && labAttendanceRepository.existsByLabScheduleIdIn(List.copyOf(scheduleIds))) {
            throw new LifecycleConflictException(
                "Attendance has already been recorded against this cohort's timetable. It can no longer be discarded.",
                "TIMETABLE_ATTENDANCE_RECORDED", "TermInstance", termInstanceId, null);
        }
        List<ClassSchedule> existing = classScheduleRepository.findByTermInstanceId(termInstanceId).stream()
            .filter(cs -> scheduleIds.contains(cs.getId()))
            .toList();
        classScheduleRepository.deleteAll(existing);
        auditLogService.record(actor, "TIMETABLE_DISCARDED", "TermInstance",
            termInstanceId.toString(), existing.size() + " session(s) discarded for cohort(s) " + cohortIds);
        return new TimetableActionResponse(existing.size());
    }

    @Transactional
    public TimetableActionResponse approve(Long termInstanceId, List<Long> cohortIds, String actor,
                                            boolean overrideIncompleteCoverage, String overrideReason) {
        TermInstance term = requireTermInstance(termInstanceId);
        requireNotLocked(term);
        requireCohortIds(cohortIds);
        Set<Long> scheduleIds = resolveCohortScheduleIds(termInstanceId, cohortIds);

        List<ClassSchedule> drafts = classScheduleRepository
            .findByTermInstanceIdAndStatusAndIsActiveTrue(termInstanceId, ClassScheduleStatus.DRAFT).stream()
            .filter(cs -> scheduleIds.contains(cs.getId()))
            .toList();
        if (drafts.isEmpty()) {
            throw new ResourceNotFoundException("No draft timetable found for the selected cohort(s) in term instance id: " + termInstanceId);
        }
        // Confirming the timetable is what finalizes staffing now -- auto-resolve whatever Skeleton
        // Builder's own placement pass couldn't (see TimetableStaffingAutoAssignService), so the
        // common case never needs a manual detour before Approve. Each staffCell call inside this
        // runs its own REQUIRES_NEW transaction/persistence context (see staffCell's own doc), so the
        // `drafts` list above is now stale for whichever rows it just staffed -- re-fetch before
        // computing what's still actually missing. Runs whole-term (auto-staffing an unrelated
        // cohort's rows is harmless -- it only ever fills gaps), only the re-fetch below is scoped.
        timetableStaffingAutoAssignService.autoStaff(termInstanceId);
        drafts = classScheduleRepository
            .findByTermInstanceIdAndStatusAndIsActiveTrue(termInstanceId, ClassScheduleStatus.DRAFT).stream()
            .filter(cs -> scheduleIds.contains(cs.getId()))
            .toList();
        // A skeleton cell with no faculty yet would otherwise fail with a raw
        // chk_class_schedule_session_shape violation the moment its status flips to PUBLISHED --
        // catch it here first with a message that actually tells the admin what to go do (open
        // Skeleton Builder and reassign faculty on the flagged session) instead of a database error.
        // LIBRARY rows are deliberately never staffed (see
        // TimetableGlobalAutoScheduleService#fillLibraryGaps) -- they publish with just a
        // classroom, no faculty, so they must not count as "still needs staffing" here.
        long unstaffedCount = drafts.stream()
            .filter(cs -> cs.getFaculty() == null)
            .filter(cs -> cs.getSessionType() != ClassSessionType.LIBRARY)
            .count();
        if (unstaffedCount > 0) {
            throw new LifecycleConflictException(
                unstaffedCount + " session(s) couldn't be auto-staffed and still need faculty assigned in Skeleton Builder before this can be approved.",
                "TIMETABLE_UNSTAFFED_CELLS", "TermInstance", termInstanceId, (int) unstaffedCount);
        }
        // Distinct from unstaffedCount above: that gate only inspects ClassSchedule rows that
        // already exist, but an offering with zero Theory faculty (or a Lab/Clinical batch with no
        // coordinator) never gets a row placed at all -- Global Auto-Schedule just drops it into the
        // unplaced-sessions report and moves on, so there's nothing there for unstaffedCount to
        // catch. This checks offering-level staffing directly instead, scoped to the selected
        // cohorts' own offerings.
        Set<Long> offeringIds = resolveCohortOfferingIds(termInstanceId, cohortIds);
        List<CourseOfferingFacultySummaryDto> assignmentSummaries =
            courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(termInstanceId).stream()
                .filter(s -> offeringIds.contains(s.offeringId()))
                .toList();
        long unassignedOfferingCount = assignmentSummaries.stream()
            .filter(s -> s.assignmentStatus() == OfferingAssignmentStatus.NONE || s.assignmentStatus() == OfferingAssignmentStatus.PARTIAL)
            .count();
        if (unassignedOfferingCount > 0) {
            throw new LifecycleConflictException(
                unassignedOfferingCount + " course offering(s) still need Theory faculty or a Lab/Clinical coordinator assigned "
                    + "via the Assign Faculty screen before it can be approved.",
                "TIMETABLE_OFFERING_UNASSIGNED_FACULTY", "TermInstance", termInstanceId, (int) unassignedOfferingCount);
        }
        // Everything below unstaffedCount is a structural correctness gate: nothing catches a
        // faculty/room double-booked across two independently-staffed skeleton cells, or a cap
        // exceeded by a later swap, until here -- see TimetableStaffingService's own class-level
        // Javadoc for why this can happen even though every individual staffCell call was clean
        // at the time. Reuses the exact same scan the Conflict Inspector dashboard shows, filtered
        // to the selected cohorts (see TimetableConflictInspectorService#scanCohorts) -- a conflict
        // entirely within an unselected cohort no longer blocks this one, but a conflict touching a
        // selected cohort's own cell still does, even if the other side belongs to a cohort that
        // isn't being published right now.
        ConflictScanResponse scan = timetableConflictInspectorService.scanCohorts(termInstanceId, cohortIds);
        if (scan.violationCount() > 0) {
            List<ConstraintViolation> violations = scan.rows().stream()
                .flatMap(row -> row.violations().stream())
                .toList();
            throw new TimetableConstraintViolationException(violations);
        }
        // A clean scan alone isn't enough: OC-258's sequential Skeleton Builder -> Conflict
        // Inspector -> Approve flow requires an admin to have actually revisited Conflict Inspector
        // after the current skeleton, not just that it happens to be clean right now (e.g. an edit
        // that introduced no new violation would satisfy the check above without anyone having
        // looked again). OC-260 scopes this per cohort -- every cohort being approved in this call
        // must individually have a fresh acknowledgment. See
        // TimetableConflictInspectorService#isCohortAcknowledgmentValid.
        for (Long cohortId : cohortIds) {
            if (!timetableConflictInspectorService.isCohortAcknowledgmentValid(termInstanceId, cohortId)) {
                throw new LifecycleConflictException(
                    "Conflict Inspector must be re-checked and its clean result acknowledged for this cohort "
                        + "before it can be approved — the skeleton has changed since the last acknowledgment.",
                    "TIMETABLE_CONFLICT_ACKNOWLEDGMENT_REQUIRED", "TermInstance", termInstanceId, null);
            }
        }
        // OC-256: none of the checks above ever compare placed hours against curriculum-required
        // hours -- a course offering that never got any Theory/Lab/Clinical sessions placed at all
        // (as opposed to placed-but-unstaffed, which unstaffedCount already catches) has nothing
        // for them to see. This is the same "Total Unassigned" figure Skeleton Builder already
        // shows per cohort, checked here (filtered to the selected cohorts, already per-cohort via
        // TimetableCoverageService#findGaps) so it can no longer reach Publish unnoticed.
        // Overridable (unlike the structural checks above) since a legitimately phased rollout is a
        // real case; TimetableController#approve is the actual enforcement point for who may
        // override.
        List<TimetableCoverageGap> coverageGaps = timetableCoverageService.findGaps(termInstanceId).stream()
            .filter(gap -> cohortIds.contains(gap.cohortId()))
            .toList();
        if (!coverageGaps.isEmpty()) {
            if (!overrideIncompleteCoverage) {
                throw new TimetableCoverageGapException(coverageGaps);
            }
            if (overrideReason == null || overrideReason.isBlank()) {
                throw new IllegalArgumentException("A reason is required to approve with incomplete curriculum-hours coverage.");
            }
        }
        for (ClassSchedule cs : drafts) {
            cs.setStatus(ClassScheduleStatus.PUBLISHED);
            classScheduleRepository.save(cs);
        }
        String auditDetail = drafts.size() + " session(s) approved for cohort(s) " + cohortIds;
        if (!coverageGaps.isEmpty()) {
            auditDetail += " -- approved with " + coverageGaps.size()
                + " incomplete-coverage gap(s) overridden (reason: " + overrideReason + ")";
        }
        auditLogService.record(actor, "TIMETABLE_APPROVED", "TermInstance", termInstanceId.toString(), auditDetail);
        return new TimetableActionResponse(drafts.size());
    }

    /**
     * Un-publishes a live timetable back to DRAFT so it can be edited/swapped and re-approved,
     * without losing the placed sessions (unlike {@link #clear}, which deletes them outright).
     * Blocked once any {@code LabAttendance} has been recorded against the selected cohort(s)'
     * sessions — {@code lab_attendances.lab_schedule_id} has no {@code ON DELETE}/status-transition
     * handling, so silently reverting attendance-backed sessions back to DRAFT would let a
     * subsequent clear/re-placement wipe out attendance history's session linkage.
     */
    @Transactional
    public TimetableActionResponse revertToDraft(Long termInstanceId, List<Long> cohortIds, String actor) {
        TermInstance term = requireTermInstance(termInstanceId);
        requireNotLocked(term);
        requireCohortIds(cohortIds);
        Set<Long> scheduleIds = resolveCohortScheduleIds(termInstanceId, cohortIds);
        List<ClassSchedule> published = classScheduleRepository
            .findByTermInstanceIdAndStatusAndIsActiveTrue(termInstanceId, ClassScheduleStatus.PUBLISHED).stream()
            .filter(cs -> scheduleIds.contains(cs.getId()))
            .toList();
        if (published.isEmpty()) {
            throw new ResourceNotFoundException("No published timetable found for the selected cohort(s) in term instance id: " + termInstanceId);
        }
        if (!scheduleIds.isEmpty() && labAttendanceRepository.existsByLabScheduleIdIn(List.copyOf(scheduleIds))) {
            throw new LifecycleConflictException(
                "Attendance has already been recorded against this cohort's timetable. It can no longer be reverted to draft.",
                "TIMETABLE_ATTENDANCE_RECORDED", "TermInstance", termInstanceId, null);
        }
        for (ClassSchedule cs : published) {
            cs.setStatus(ClassScheduleStatus.DRAFT);
            classScheduleRepository.save(cs);
        }
        auditLogService.record(actor, "TIMETABLE_REVERTED_TO_DRAFT", "TermInstance",
            termInstanceId.toString(), published.size() + " session(s) reverted to draft for cohort(s) " + cohortIds);
        return new TimetableActionResponse(published.size());
    }

    /** OC-260: decorates {@link TimetableSkeletonService#getCohortTermStatusSummary}'s rows with
     *  each cohort's Pending -&gt; Draft/Generated -&gt; Conflicts Resolved -&gt; Published readiness.
     *  Lives here
     *  rather than on {@code TimetableSkeletonService} itself because computing it needs {@link
     *  TimetableConflictInspectorService} (cohort conflict scan/acknowledgment), which in turn
     *  depends on {@code TimetableSkeletonService} for row resolution -- putting the computation on
     *  either of those two would create a circular bean dependency. This service already legitimately
     *  depends on all four gate-owning services {@link #approve} itself uses, so it's the natural
     *  place both live without a cycle. */
    @Transactional(readOnly = true)
    public Page<CohortTermStatusSummary> getCohortTermStatusSummaryWithReadiness(
            Long termInstanceId, Long cohortId, Pageable pageable) {
        Page<CohortTermStatusSummary> page = timetableSkeletonService.getCohortTermStatusSummary(termInstanceId, cohortId, pageable);
        if (page.isEmpty()) {
            return page;
        }
        // Hoisted out of the per-cohort loop below: each of these is already whole-term, so
        // computing it once and reusing it per row avoids the term getting re-scanned/re-queried
        // once per still-draft cohort. The original version called scanCohorts (a full
        // scanTerm) separately per cohort, so a 4-cohort term ran the whole-term structural scan
        // 4 times just to render 4 rows -- visibly slow in practice, fixed here.
        List<CourseOfferingFacultySummaryDto> assignmentSummaries =
            courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(termInstanceId);
        List<TimetableCoverageGap> coverageGaps = timetableCoverageService.findGaps(termInstanceId);
        ConflictScanResponse termScan = timetableConflictInspectorService.scanTerm(termInstanceId);
        List<CohortTermStatusSummary> decorated = page.getContent().stream()
            .map(row -> new CohortTermStatusSummary(
                row.cohortId(), row.cohortName(), row.courseName(), row.admissionYearName(),
                computeReadinessStatus(termInstanceId, row, assignmentSummaries, coverageGaps, termScan),
                row.draftCount(), row.publishedCount(), row.unassignedHours(),
                isAttendanceRecorded(termInstanceId, row.cohortId())))
            .toList();
        return new PageImpl<>(decorated, pageable, page.getTotalElements());
    }

    /** Both Discard and Revert-to-Draft permanently refuse once this is true (see their own guards
     *  above) -- computed here, per row, so the row table can hide those actions upfront rather
     *  than offering a button that can only ever fail. */
    private boolean isAttendanceRecorded(Long termInstanceId, Long cohortId) {
        Set<Long> scheduleIds = resolveCohortScheduleIds(termInstanceId, List.of(cohortId));
        return !scheduleIds.isEmpty() && labAttendanceRepository.existsByLabScheduleIdIn(List.copyOf(scheduleIds));
    }

    private String computeReadinessStatus(Long termInstanceId, CohortTermStatusSummary row,
                                           List<CourseOfferingFacultySummaryDto> assignmentSummaries,
                                           List<TimetableCoverageGap> coverageGaps,
                                           ConflictScanResponse termScan) {
        if ("PUBLISHED".equals(row.status()) || "PARTIALLY_PUBLISHED".equals(row.status())) {
            return row.status();
        }
        if (row.draftCount() == 0 && row.publishedCount() == 0) {
            return "PENDING";
        }
        // Otherwise ready ("CONFLICTS_RESOLVED") only once every one of approve()'s own preflight
        // gates would currently pass for this cohort -- checked in the same cheapest-first order as
        // approve() itself, short-circuiting on the first failure.
        Long cohortId = row.cohortId();
        List<ClassSchedule> cells = timetableSkeletonService.getCohortActiveClassSchedules(termInstanceId, cohortId);
        boolean unstaffed = cells.stream()
            .filter(cs -> cs.getStatus() == ClassScheduleStatus.DRAFT)
            .anyMatch(cs -> cs.getFaculty() == null && cs.getSessionType() != ClassSessionType.LIBRARY);
        if (unstaffed) {
            return "DRAFTED";
        }
        Set<Long> offeringIds = resolveCohortOfferingIds(termInstanceId, List.of(cohortId));
        boolean offeringGap = assignmentSummaries.stream()
            .filter(s -> offeringIds.contains(s.offeringId()))
            .anyMatch(s -> s.assignmentStatus() == OfferingAssignmentStatus.NONE || s.assignmentStatus() == OfferingAssignmentStatus.PARTIAL);
        if (offeringGap) {
            return "DRAFTED";
        }
        boolean coverageGap = coverageGaps.stream().anyMatch(gap -> gap.cohortId().equals(cohortId));
        if (coverageGap) {
            return "DRAFTED";
        }
        if (timetableConflictInspectorService.filterScanForCohorts(termScan, termInstanceId, List.of(cohortId)).violationCount() > 0) {
            return "DRAFTED";
        }
        if (!timetableConflictInspectorService.isCohortAcknowledgmentValid(termInstanceId, cohortId)) {
            return "DRAFTED";
        }
        return "CONFLICTS_RESOLVED";
    }
}
