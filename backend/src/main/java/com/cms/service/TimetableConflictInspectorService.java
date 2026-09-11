package com.cms.service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConflictScanResponse;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.TimetableConflictRow;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.Room;
import com.cms.model.TermInstance;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.ClassScheduleRepository;
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

    public TimetableConflictInspectorService(ClassScheduleRepository classScheduleRepository,
                                              TermInstanceRepository termInstanceRepository,
                                              TimetableStaffingService timetableStaffingService,
                                              TimetableBlockedPeriodChecker blockedPeriodChecker,
                                              TimetableClinicalShiftChecker clinicalShiftChecker,
                                              CourseOfferingService courseOfferingService,
                                              StudentTermEnrollmentRepository studentTermEnrollmentRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.timetableStaffingService = timetableStaffingService;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.clinicalShiftChecker = clinicalShiftChecker;
        this.courseOfferingService = courseOfferingService;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
    }

    public ConflictScanResponse scanTerm(Long termInstanceId) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        List<ClassSchedule> cells = classScheduleRepository.findByTermInstanceId(termInstanceId).stream()
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
            case THEORY, LIBRARY -> cs.getClassroom() != null ? cs.getClassroom().getCapacity() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getCapacity() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getCapacity() : null;
        };
    }

    private TimetableConflictRow toRow(ClassSchedule cs, List<ConstraintViolation> violations) {
        var period = cs.getPeriod();
        Long venueId = TimetableStaffingService.venueIdOf(cs);
        String venueName = switch (cs.getSessionType()) {
            case THEORY, LIBRARY -> cs.getClassroom() != null ? cs.getClassroom().getName() : null;
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
