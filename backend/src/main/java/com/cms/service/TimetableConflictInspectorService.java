package com.cms.service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConflictAcknowledgmentStatusResponse;
import com.cms.dto.ConflictScanResponse;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.TimetableConflictRow;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.ClassSchedule;
import com.cms.model.CohortConflictAcknowledgment;
import com.cms.model.Faculty;
import com.cms.model.Room;
import com.cms.model.TermInstance;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortConflictAcknowledgmentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Whole-term "is anything actually wrong right now" scan — a defense-in-depth counterpart to
 * {@link TimetableStaffingService}, which only ever validates one incoming placement/staff
 * attempt at a time. Nothing here is new conflict logic: every check is the same non-throwing,
 * already-validated method {@code staffCell} itself uses, re-run against every already-placed
 * cell in the term instead of just the one being edited. {@link TimetableGenerationService#approve}
 * uses this same scan as its publish gate, so "the dashboard is clean" and "publish is allowed"
 * are guaranteed to mean the same thing.
 */
@Service
@Transactional(readOnly = true)
public class TimetableConflictInspectorService {

    private final ClassScheduleRepository classScheduleRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final TimetableStaffingService timetableStaffingService;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;

    private final TimetableClinicalShiftChecker clinicalShiftChecker;
    private final CourseOfferingService courseOfferingService;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final TimetableSkeletonService timetableSkeletonService;
    private final CohortConflictAcknowledgmentRepository cohortConflictAcknowledgmentRepository;

    public TimetableConflictInspectorService(ClassScheduleRepository classScheduleRepository,
                                              TermInstanceRepository termInstanceRepository,
                                              TimetableStaffingService timetableStaffingService,
                                              TimetableBlockedPeriodChecker blockedPeriodChecker,
                                              TimetableClinicalShiftChecker clinicalShiftChecker,
                                              CourseOfferingService courseOfferingService,
                                              StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                              TimetableSkeletonService timetableSkeletonService,
                                              CohortConflictAcknowledgmentRepository cohortConflictAcknowledgmentRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.timetableStaffingService = timetableStaffingService;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.clinicalShiftChecker = clinicalShiftChecker;
        this.courseOfferingService = courseOfferingService;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.timetableSkeletonService = timetableSkeletonService;
        this.cohortConflictAcknowledgmentRepository = cohortConflictAcknowledgmentRepository;
    }

    /** OC-260: union of every active {@link ClassSchedule} id belonging to any of the given
     *  cohorts, via {@link TimetableSkeletonService#getCohortActiveClassSchedules} — the same
     *  per-cohort row resolution the rest of the cohort-scoped publish lifecycle uses. */
    private Set<Long> resolveCohortScheduleIds(Long termInstanceId, List<Long> cohortIds) {
        Set<Long> ids = new HashSet<>();
        for (Long cohortId : cohortIds) {
            timetableSkeletonService.getCohortActiveClassSchedules(termInstanceId, cohortId)
                .forEach(cs -> ids.add(cs.getId()));
        }
        return ids;
    }

    /** Cohort-scoped view of {@link #scanTerm}: the underlying scan must still run whole-term
     *  (cross-cohort context like shared electives/faculty is needed to detect a real conflict
     *  between two cohorts), but the returned rows are filtered to cells that belong to one of the
     *  given cohorts — so a conflict entirely within a cohort that isn't being published/checked
     *  right now never blocks this one. A conflict straddling a selected cohort and an unselected
     *  one still surfaces here, because the selected cohort's own cell is one of the flagged rows. */
    public ConflictScanResponse scanCohorts(Long termInstanceId, List<Long> cohortIds) {
        return filterScanForCohorts(scanTerm(termInstanceId), termInstanceId, cohortIds);
    }

    /** Same filtering {@link #scanCohorts} does, but against an already-computed {@link
     *  #scanTerm} result instead of running a fresh one -- for a caller that needs this for
     *  several different cohort subsets within one request (e.g. {@code
     *  TimetableGenerationService#getCohortTermStatusSummaryWithReadiness}, computing every
     *  cohort's own readiness for a term) so the whole-term scan runs exactly once instead of once
     *  per cohort. OC-260 originally had the summary endpoint call {@link #scanCohorts} once per
     *  still-draft cohort, which meant scanning the entire term N times just to render N rows —
     *  visibly slow with more than a couple of cohorts. */
    public ConflictScanResponse filterScanForCohorts(ConflictScanResponse scan, Long termInstanceId, List<Long> cohortIds) {
        Set<Long> scheduleIds = resolveCohortScheduleIds(termInstanceId, cohortIds);
        List<TimetableConflictRow> rows = scan.rows().stream()
            .filter(row -> scheduleIds.contains(row.classScheduleId()))
            .toList();
        Map<String, Integer> countsByCode = new TreeMap<>();
        int totalViolations = 0;
        for (TimetableConflictRow row : rows) {
            for (ConstraintViolation violation : row.violations()) {
                countsByCode.merge(violation.code(), 1, Integer::sum);
                totalViolations++;
            }
        }
        return new ConflictScanResponse(termInstanceId, scan.termLabel(), scan.scannedAt(),
            scheduleIds.size(), rows.size(), totalViolations, countsByCode, rows);
    }

    /** Cohort-scoped sibling of {@link #acknowledge(Long)} — re-scans just this cohort via {@link
     *  #scanCohorts} and, if clean, upserts its own {@link CohortConflictAcknowledgment} row rather
     *  than the whole term's single acknowledgment. */
    @Transactional
    public ConflictAcknowledgmentStatusResponse acknowledgeCohort(Long termInstanceId, Long cohortId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        ConflictScanResponse scan = scanCohorts(termInstanceId, List.of(cohortId));
        if (scan.violationCount() > 0) {
            List<ConstraintViolation> violations = scan.rows().stream()
                .flatMap(row -> row.violations().stream())
                .toList();
            throw new TimetableConstraintViolationException(violations);
        }
        Instant now = Instant.now();
        CohortConflictAcknowledgment ack = cohortConflictAcknowledgmentRepository
            .findByTermInstanceIdAndCohortId(termInstanceId, cohortId)
            .orElseGet(CohortConflictAcknowledgment::new);
        ack.setTermInstanceId(termInstanceId);
        ack.setCohortId(cohortId);
        ack.setAcknowledgedAt(now);
        ack.setAcknowledgedCellCount(scan.scannedCellCount());
        cohortConflictAcknowledgmentRepository.save(ack);
        return new ConflictAcknowledgmentStatusResponse(termInstanceId, true, now);
    }

    public ConflictAcknowledgmentStatusResponse getCohortAcknowledgmentStatus(Long termInstanceId, Long cohortId) {
        boolean valid = isCohortAcknowledgmentValid(termInstanceId, cohortId);
        Instant acknowledgedAt = valid
            ? cohortConflictAcknowledgmentRepository.findByTermInstanceIdAndCohortId(termInstanceId, cohortId)
                .map(CohortConflictAcknowledgment::getAcknowledgedAt).orElse(null)
            : null;
        return new ConflictAcknowledgmentStatusResponse(termInstanceId, valid, acknowledgedAt);
    }

    /** Cohort-scoped sibling of {@link #isAcknowledgmentValid(TermInstance)}: valid only when this
     *  exact cohort has been acknowledged and neither its active cell count nor its latest edit
     *  timestamp has changed since — same staleness rule, scoped to just this cohort's own rows
     *  instead of the whole term's. */
    public boolean isCohortAcknowledgmentValid(Long termInstanceId, Long cohortId) {
        Optional<CohortConflictAcknowledgment> ackOpt =
            cohortConflictAcknowledgmentRepository.findByTermInstanceIdAndCohortId(termInstanceId, cohortId);
        if (ackOpt.isEmpty()) {
            return false;
        }
        CohortConflictAcknowledgment ack = ackOpt.get();
        List<ClassSchedule> cells = timetableSkeletonService.getCohortActiveClassSchedules(termInstanceId, cohortId);
        if (cells.size() != ack.getAcknowledgedCellCount()) {
            return false;
        }
        return cells.stream()
            .map(ClassSchedule::getUpdatedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .map(maxUpdatedAt -> !maxUpdatedAt.isAfter(ack.getAcknowledgedAt()))
            .orElse(true);
    }

    /** Binds an {@link AutoScheduleRunCache} for the duration of the scan so the per-cell checks
     *  this reuses (see the class javadoc) hit an in-memory snapshot instead of re-querying the
     *  same term-wide/cohort-wide data once per cell -- a 4-cohort, ~118-session term measured at
     *  10+ seconds before this, dominated by {@link TimetableClinicalShiftChecker} re-resolving a
     *  cohort's shift windows from scratch on every one of that cohort's cells. Skips binding a new
     *  cache when one is already active on this thread (e.g. {@link
     *  TimetableGlobalAutoScheduleService}'s own post-run conflict report calls this while its run
     *  is still in progress) -- {@link AutoScheduleRunCache#run} refuses to nest, and reusing the
     *  caller's already-current snapshot is correct there anyway, not just exception-avoidance. */
    public ConflictScanResponse scanTerm(Long termInstanceId) {
        if (AutoScheduleRunCache.current().isPresent()) {
            return scanTermUncached(termInstanceId);
        }
        return AutoScheduleRunCache.run(termInstanceId, classScheduleRepository, () -> scanTermUncached(termInstanceId));
    }

    private ConflictScanResponse scanTermUncached(Long termInstanceId) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        // Active rows only: every auto-schedule rebuild switches the previous run's draft off rather
        // than deleting it, and each switched-off copy sits in the same slot as its replacement --
        // scanning them reported every rebuilt session as a double-booking of itself. Reuses the
        // active AutoScheduleRunCache's own already-loaded, already-active-filtered snapshot when
        // one is bound (always true for a standalone call -- see #scanTerm) instead of running a
        // second separate query here.
        List<ClassSchedule> cells = AutoScheduleRunCache.current()
            .map(AutoScheduleRunCache::allCells)
            .orElseGet(() -> classScheduleRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId))
            .stream()
            .filter(cs -> cs.getPeriod() != null)
            .toList();

        List<TimetableConflictRow> rows = new ArrayList<>();
        Map<String, Integer> countsByCode = new TreeMap<>();

        // Built once for the whole scan: which cohorts each offering actually serves. Needed only
        // for cells that carry no CohortSection (electives are placed per cohort, not per section),
        // where the audience cohort isn't reachable from the row itself. Resolving it per row would
        // mean one query set per cell across the entire term.
        Map<Long, Set<Long>> cohortsByOffering = buildCohortsByOffering(termInstanceId);

        for (ClassSchedule cs : cells) {
            List<ConstraintViolation> violations = checkCell(cs, term, cohortsByOffering);
            if (violations.isEmpty()) {
                continue;
            }
            rows.add(toRow(cs, violations));
            for (ConstraintViolation violation : violations) {
                countsByCode.merge(violation.code(), 1, Integer::sum);
            }
        }

        int totalViolations = countsByCode.values().stream().mapToInt(Integer::intValue).sum();

        return new ConflictScanResponse(
            termInstanceId,
            termLabel(term),
            Instant.now(),
            cells.size(),
            rows.size(),
            totalViolations,
            countsByCode,
            rows
        );
    }

    /** offeringId -> the cohorts enrolled in this term that actually take it. Inverts
     *  {@code CourseOfferingService#getOfferingsByTermInstanceAndCohort}, which only runs the
     *  cohort-to-offerings direction, by walking each enrolled cohort once. */
    private Map<Long, Set<Long>> buildCohortsByOffering(Long termInstanceId) {
        Map<Long, Set<Long>> byOffering = new HashMap<>();
        for (Long cohortId : studentTermEnrollmentRepository
                .findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED)) {
            for (CourseOfferingDto offering : courseOfferingService.getOfferingsByTermInstanceAndCohort(termInstanceId, cohortId)) {
                byOffering.computeIfAbsent(offering.id(), k -> new HashSet<>()).add(cohortId);
            }
        }
        return byOffering;
    }

    private List<ConstraintViolation> checkCell(ClassSchedule cs, TermInstance term, Map<Long, Set<Long>> cohortsByOffering) {
        LocalTime start = cs.getPeriod().getStartTime();
        LocalTime end = cs.getPeriod().getEndTime();
        List<ConstraintViolation> violations = new ArrayList<>();

        blockedPeriodChecker.blockReason(cs.getDayOfWeek(), start, end, term)
            .ifPresent(reason -> violations.add(new ConstraintViolation(
                "CONFLICT_PERIOD_BLOCKED", "This day and period is blocked: " + reason)));

        // A session sitting inside its own cohort's Clinical Shift duty window. Every placement
        // path already refuses this, but nothing revalidates rows that were legal when placed and
        // stopped being legal afterwards -- editing a shift group's start time, duration or travel
        // buffer moves the window underneath an already-built week and revalidates nothing. Such a
        // row stays active, is not drawn in the Skeleton Builder grid at all (its period collapses
        // into the duty banner), and before this check it also passed the scan that gates Publish,
        // so it could reach a published timetable entirely unseen.
        Set<Long> fallbackCohorts = cs.getCourseOffering() != null
            ? cohortsByOffering.getOrDefault(cs.getCourseOffering().getId(), Set.of())
            : Set.of();
        clinicalShiftChecker.blockReasonForCell(cs, fallbackCohorts)
            .ifPresent(v -> violations.add(new ConstraintViolation("CONFLICT_CLINICAL_SHIFT_BLOCKED", v.message())));

        Faculty faculty = cs.getFaculty();
        if (faculty != null) {
            timetableStaffingService.checkFacultyAvailable(faculty.getId(), cs.getDayOfWeek(), start, end, null)
                .ifPresent(violations::add);
            timetableStaffingService.checkFacultyFree(faculty.getId(), cs, cs.getDayOfWeek(), start, end)
                .ifPresent(violations::add);
            violations.addAll(timetableStaffingService.checkWithinWorkloadCaps(faculty, cs, cs.getDayOfWeek(), start, end));
        }

        Long venueId = TimetableStaffingService.venueIdOf(cs);
        if (venueId != null) {
            Room physicalRoom = TimetableStaffingService.physicalRoomOf(cs);
            timetableStaffingService.checkRoomFree(cs.getSessionType(), venueId, physicalRoom, cs, cs.getDayOfWeek(), start, end)
                .ifPresent(violations::add);
            timetableStaffingService.checkCapacityFit(cs, venueCapacityOf(cs))
                .ifPresent(violations::add);
        }

        return violations;
    }

    private Integer venueCapacityOf(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY, LIBRARY, SPORTS -> cs.getClassroom() != null ? cs.getClassroom().getCapacity() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getCapacity() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getCapacity() : null;
        };
    }

    private TimetableConflictRow toRow(ClassSchedule cs, List<ConstraintViolation> violations) {
        var period = cs.getPeriod();
        Long venueId = TimetableStaffingService.venueIdOf(cs);
        String venueName = switch (cs.getSessionType()) {
            case THEORY, LIBRARY, SPORTS -> cs.getClassroom() != null ? cs.getClassroom().getName() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getName() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getName() : null;
        };

        return new TimetableConflictRow(
            cs.getId(),
            cs.getSubject().getName(),
            cs.getSubject().getCode(),
            cs.getSessionType(),
            cs.getDayOfWeek(),
            period.getName(),
            period.getStartTime(),
            period.getEndTime(),
            cs.getFaculty() != null ? cs.getFaculty().getFullName() : null,
            venueId != null ? venueName : null,
            cs.getBatchName() != null ? cs.getBatchName()
                : (cs.getBatch() != null ? cs.getBatch().getName()
                : (cs.getCohortSection() != null ? cs.getCohortSection().getSectionLabel() : null)),
            cs.getStatus(),
            violations
        );
    }

    private String termLabel(TermInstance term) {
        return term.getAcademicYear().getName() + " " + term.getTermType();
    }
}
